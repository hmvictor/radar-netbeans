package qubexplorer.runner;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
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
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.UserCredentials;
import qubexplorer.server.SonarQube;
import qubexplorer.server.Version;
import qubexplorer.ui.ProjectContext;

/**
 *
 * @author Victor
 */
public class SonarRunnerProccess {

    protected static final String JSON_FILENAME = "sonar-report.json";

    public enum AnalysisMode {

        INCREMENTAL,
        PREVIEW;

    }

    private final ProjectContext projectContext;
    private final String sonarUrl;
    private AnalysisMode analysisMode = AnalysisMode.INCREMENTAL;

    private PrintStreamConsumer outConsumer;
    private PrintStreamConsumer errConsumer;
    private WrapperConsumer wrapper;
    private List<String> jvmArguments = Collections.emptyList();

    private final List<VersionConfig> versionConfigs = Collections.unmodifiableList(Arrays.asList(new VersionConfigLessThan4(), new VersionConfigLessThan5Point2(), new VersionConfigMoreThan5Point2()));

    /**
     * This state is modified while running.
     */
    private String projectHome;
    private final Properties properties = new Properties();

    public SonarRunnerProccess(String sonarUrl, ProjectContext projectContext) {
        this.sonarUrl = sonarUrl;
        this.projectContext = projectContext;
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
        ForkedRunner runner = ForkedRunner.create(processMonitor);
        projectHome = projectContext.getProject().getProjectDirectory().getPath();
        configureProperties(userCredentials);
        runner.addProperties(properties);
        if (outConsumer != null) {
            runner.setStdOut(outConsumer);
        }
        wrapper = new WrapperConsumer(errConsumer);
        runner.setStdErr(wrapper);
        runner.addJvmArguments(jvmArguments);
        return runner;
    }

    private void configureProperties(UserCredentials userCredentials) throws MvnModelInputException {
        SonarQubeProjectConfiguration projectConfiguration = projectContext.getConfiguration();
        Version sonarQubeVersion = new SonarQube(sonarUrl).getVersion(userCredentials);
        Module mainModule = Module.createMainModule(projectContext);

        //mainModule.loadExternalProperties(properties);
        properties.setProperty("sonar.projectBaseDir", projectHome);
        properties.setProperty("sonar.host.url", sonarUrl);
        properties.setProperty("sonar.projectDir", projectHome);
        properties.setProperty("project.home", projectHome);
//        properties.setProperty("sonar.projectName", projectConfiguration.getName());
        properties.setProperty("sonar.projectVersion", projectConfiguration.getVersion());
        properties.setProperty("sonar.sourceEncoding", FileEncodingQuery.getEncoding(projectContext.getProject().getProjectDirectory()).displayName());
        properties.setProperty("sonar.working.directory", getWorkingDirectory());

        //optional properties
        if (userCredentials != null) {
            properties.setProperty("sonar.login", userCredentials.getUsername());
            properties.setProperty("sonar.password", PassEncoder.decodeAsString(userCredentials.getPassword()));
        }
        String sourceLevel = SourceLevelQuery.getSourceLevel(projectContext.getProject().getProjectDirectory());
        if (sourceLevel != null) {
            properties.setProperty("sonar.java.source", sourceLevel);
        }
        if (SonarMvnProject.isMvnProject(projectContext.getProject())) {
            properties.setProperty("sonar.junit.reportsPath", new File(SonarMvnProject.getOutputDirectory(projectContext.getProject()), "/surefire-reports").getAbsolutePath());
        }
        //end optional

        for (VersionConfig versionConfig : getVersionConfigsFor(sonarQubeVersion)) {
            versionConfig.apply(this, properties);
        }
        mainModule.addModuleProperties(sonarQubeVersion, properties);
//        mainModule.configureSourcesAndBinariesProperties(sonarQubeVersion, properties);
        ModulesConfigurationResult result = configureModulesProperties(mainModule, sonarQubeVersion);
        if (!result.hasModulesWithSources() && !mainModule.containsSources()) {
            throw new SourcesNotFoundException();
        }
        properties.setProperty("sonar.projectKey", result.hasSubmodules() ? projectConfiguration.getKey().getPart(0) : projectConfiguration.getKey().toString());
    }

    private List<VersionConfig> getVersionConfigsFor(Version sonarQubeVersion) {
        List<VersionConfig> list = new LinkedList<>();
        for (VersionConfig config : versionConfigs) {
            if (config.applies(sonarQubeVersion)) {
                list.add(config);
            }
        }
        return list;
    }

    public String getWorkingDirectory() throws MvnModelInputException {
        String workingDirectory;
        if (SonarMvnProject.isMvnProject(projectContext.getProject())) {
            workingDirectory = new File(SonarMvnProject.getOutputDirectory(projectContext.getProject()), "sonar").getAbsolutePath();
        } else {
            workingDirectory = projectHome + "/./.sonar";
        }
        return workingDirectory;
    }

    private ModulesConfigurationResult configureModulesProperties(Module mainModule, Version sonarQubeVersion) throws MvnModelInputException {
        int sourcesCounter = 0;
        StringBuilder modulesWithSources = new StringBuilder();
        Set<Project> subprojects = getSubprojects();
        for (Project subproject : subprojects) {
            Module module = mainModule.createSubmodule(subproject);
            module.addModuleProperties(sonarQubeVersion, properties);
            if (module.containsSources()) {
                if (modulesWithSources.length() > 0) {
                    modulesWithSources.append(',');
                }
                modulesWithSources.append(module.getName());
                sourcesCounter++;
            }
        }
        if (modulesWithSources.length() > 0) {
            properties.setProperty("sonar.modules", modulesWithSources.toString());
        }
        return new ModulesConfigurationResult(!subprojects.isEmpty(), sourcesCounter > 0);
    }

    public Set<Project> getSubprojects() {
        Set<Project> subprojects = ProjectUtils.getContainedProjects(projectContext.getProject(), true);
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
            File jsonFile = new File(properties.getProperty("sonar.working.directory"), JSON_FILENAME);
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

    private static class ModulesConfigurationResult {

        private final boolean hasModules;
        private final boolean hasModulesWithSources;

        public ModulesConfigurationResult(boolean hasModules, boolean hasModulesWithSources) {
            this.hasModules = hasModules;
            this.hasModulesWithSources = hasModulesWithSources;
        }

        public boolean hasSubmodules() {
            return hasModules;
        }

        public boolean hasModulesWithSources() {
            return hasModulesWithSources;
        }
        
    }

}
