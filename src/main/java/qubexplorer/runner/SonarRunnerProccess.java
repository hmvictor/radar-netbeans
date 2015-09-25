package qubexplorer.runner;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import org.netbeans.api.java.queries.SourceLevelQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.queries.FileEncodingQuery;
import org.sonar.runner.api.ForkedRunner;
import org.sonar.runner.api.PrintStreamConsumer;
import org.sonar.runner.api.ProcessMonitor;
import org.sonar.runner.api.Runner;
import qubexplorer.AuthorizationException;
import qubexplorer.MvnModelInputException;
import qubexplorer.PassEncoder;
import qubexplorer.SonarMvnProject;
import qubexplorer.SonarQubeProjectBuilder;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.UserCredentials;
import qubexplorer.server.SonarQube;
import qubexplorer.server.Version;

/**
 *
 * @author Victor
 */
public class SonarRunnerProccess {

    private final Project project;
    private final String sonarUrl;
    private AnalysisMode analysisMode = AnalysisMode.INCREMENTAL;
    private PrintStreamConsumer outConsumer;
    private PrintStreamConsumer errConsumer;
    private WrapperConsumer wrapper;
    private List<String> jvmArguments = Collections.emptyList();

    public enum AnalysisMode {

        INCREMENTAL,
        PREVIEW;
    }

    /**
     * This state is modified while running.
     */
    private String projectHome;
    private final Properties properties = new Properties();

    public SonarRunnerProccess(String sonarUrl, Project project) {
        this.sonarUrl = sonarUrl;
        this.project = project;
    }

    public PrintStreamConsumer getOutConsumer() {
        return outConsumer;
    }

    public void setOutConsumer(PrintStreamConsumer outConsumer) {
        this.outConsumer = outConsumer;
    }

    public PrintStreamConsumer getErrConsumer() {
        return errConsumer;
    }

    public void setErrConsumer(PrintStreamConsumer errConsumer) {
        this.errConsumer = errConsumer;
    }

    public AnalysisMode getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(AnalysisMode analysisMode) {
        Objects.requireNonNull(analysisMode, "analysisMode is null");
        this.analysisMode = analysisMode;
    }

    public List<String> getJvmArguments() {
        return jvmArguments;
    }

    public void setJvmArguments(List<String> jvmArguments) {
        Objects.requireNonNull(jvmArguments, "argument list is null");
        this.jvmArguments = jvmArguments;
    }

    protected Runner createRunnerForProject(UserCredentials userCredentials, ProcessMonitor processMonitor) throws MvnModelInputException {
        int sourcesCounter = 0;
        ForkedRunner runner = ForkedRunner.create(processMonitor);
        projectHome = project.getProjectDirectory().getPath();
        SonarQubeProjectConfiguration projectInfo = SonarQubeProjectBuilder.getDefaultConfiguration(project);
        properties.setProperty("sonar.projectName", projectInfo.getName());
        properties.setProperty("sonar.projectBaseDir", projectHome);
        properties.setProperty("sonar.projectVersion", projectInfo.getVersion());
        properties.setProperty("sonar.sourceEncoding", FileEncodingQuery.getEncoding(project.getProjectDirectory()).displayName());
        properties.setProperty("sonar.host.url", sonarUrl);
        String sourceLevel = SourceLevelQuery.getSourceLevel(project.getProjectDirectory());
        if (sourceLevel != null) {
            properties.setProperty("sonar.java.source", sourceLevel);
        }
        properties.setProperty("sonar.projectDir", projectHome);
        properties.setProperty("project.home", projectHome);
        Version sonarQubeVersion = new SonarQube(sonarUrl).getVersion(userCredentials);
        if (sonarQubeVersion.getMajor() >= 4) {
            properties.setProperty("sonar.analysis.mode", analysisMode.toString().toLowerCase());
        } else {
            properties.setProperty("sonar.dryRun", "true");
        }
        String workingDirectory;
        if (SonarMvnProject.isMvnProject(project)) {
            File outputDirectory = SonarMvnProject.getOutputDirectory(project);
            workingDirectory = new File(outputDirectory, "sonar").getAbsolutePath();
            properties.setProperty("sonar.junit.reportsPath", new File(outputDirectory, "/surefire-reports").getAbsolutePath());
        } else {
            workingDirectory = projectHome + "/./.sonar";
        }
        properties.setProperty("sonar.working.directory", workingDirectory);
        if (userCredentials != null) {
            properties.setProperty("sonar.login", userCredentials.getUsername());
            properties.setProperty("sonar.password", PassEncoder.decodeAsString(userCredentials.getPassword()));
        }
        Module mainModule = Module.createMainModule(project);
        mainModule.configureSourcesAndBinariesProperties(sonarQubeVersion, properties);
        if (mainModule.containsSources()) {
            sourcesCounter++;
        }

        StringBuilder modulesWithSources = new StringBuilder();
        Set<Project> subprojects = getSubprojects();
        for (Project subproject : subprojects) {
            Module module = Module.createSubmodule(subproject);
            module.addModuleProperties(sonarQubeVersion, properties);
            if (module.containsSources()) {
                if (modulesWithSources.length() > 0) {
                    modulesWithSources.append(',');
                }
                modulesWithSources.append(module.getName());
                sourcesCounter++;
            }
        }

        if (sourcesCounter == 0) {
            throw new SourcesNotFoundException();
        }
        assert projectInfo.getKey().getPartsCount() == 2;
        properties.setProperty("sonar.projectKey", subprojects.isEmpty() ? projectInfo.getKey().toString() : projectInfo.getKey().getPart(0));
        if (modulesWithSources.length() > 0) {
            properties.setProperty("sonar.modules", modulesWithSources.toString());
        }
        if (outConsumer != null) {
            runner.setStdOut(outConsumer);
        }
        wrapper = new WrapperConsumer(errConsumer);
        runner.setStdErr(wrapper);
        runner.addJvmArguments(jvmArguments);
        runner.addProperties(properties);
        return runner;
    }

    public Set<Project> getSubprojects() {
        Set<Project> subprojects = ProjectUtils.getContainedProjects(project, true);
        if (subprojects == null) {
            subprojects = Collections.emptySet();
        }
        return subprojects;
    }

    public SonarRunnerResult executeRunner(UserCredentials credentials, ProcessMonitor processMonitor) throws MvnModelInputException {
        Runner runner = createRunnerForProject(credentials, processMonitor);
        try {
            runner.execute();
        } catch (Exception ex) {
            if (wrapper.isUnauthorized()) {
                throw new AuthorizationException();
            } else {
                throw new SonarRunnerException(ex);
            }
        }
        if (processMonitor.stop()) {
            throw new SonarRunnerCancelledException();
        } else {
            File jsonFile = new File(properties.getProperty("sonar.working.directory"), "sonar-report.json");
            if (!jsonFile.exists()) {
                throw new SonarRunnerException("No result file");
            } else {
                return new SonarRunnerResult(jsonFile);
            }
        }
    }

    private static class WrapperConsumer extends PrintStreamConsumer {

        private boolean unauthorized;
        private final PrintStreamConsumer wrapee;

        public WrapperConsumer(PrintStreamConsumer consumer) {
            super(null);
            this.wrapee = consumer;
        }

        public boolean isUnauthorized() {
            return unauthorized;
        }

        @Override
        public void consumeLine(String line) {
            if (line.toLowerCase().contains("not authorized")) {
                unauthorized = true;
            }
            if (wrapee != null) {
                wrapee.consumeLine(line);
            }
        }

    }

}
