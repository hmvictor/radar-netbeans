package qubexplorer.runner;

import java.io.File;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.model.Model;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import org.netbeans.api.java.queries.SourceLevelQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.spi.project.SubprojectProvider;
import org.openide.util.Utilities;
import org.sonar.runner.api.ForkedRunner;
import org.sonar.runner.api.PrintStreamConsumer;
import org.sonar.runner.api.ProcessMonitor;
import org.sonar.runner.api.Runner;
import qubexplorer.UserCredentials;
import qubexplorer.AuthorizationException;
import qubexplorer.MvnModelFactory;
import qubexplorer.MvnModelInputException;
import qubexplorer.PassEncoder;
import qubexplorer.server.SonarQube;

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

    public enum AnalysisMode {

        INCREMENTAL,
        PREVIEW;
    }

    /**
     * This state is modified while running.
     */
    private final MvnModelFactory mvnModelFactory = new MvnModelFactory();
    private String projectHome;
    private final StringBuilder modules = new StringBuilder();
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

    protected Runner createForProject(UserCredentials userCredentials, ProcessMonitor processMonitor) throws MvnModelInputException {
        int sourcesCounter = 0;
        ForkedRunner runner = ForkedRunner.create(processMonitor);
        projectHome = project.getProjectDirectory().getPath();
        Model model = mvnModelFactory.createModel(project);
        properties.setProperty("sonar.projectName", model.getName() != null ? model.getName() : model.getArtifactId());
        properties.setProperty("sonar.projectBaseDir", projectHome);
        properties.setProperty("sonar.projectVersion", model.getVersion());
        properties.setProperty("sonar.sourceEncoding", FileEncodingQuery.getEncoding(project.getProjectDirectory()).displayName());
        properties.setProperty("sonar.host.url", sonarUrl);
        properties.setProperty("sonar.java.source", SourceLevelQuery.getSourceLevel(project.getProjectDirectory()));
        int version = getMajorVersion(new SonarQube(sonarUrl).getVersion(userCredentials));
        if (version >= 4) {
            properties.setProperty("sonar.analysis.mode", analysisMode.toString().toLowerCase());
        } else {
            properties.setProperty("sonar.dryRun", "true");
        }
        properties.setProperty("sonar.projectDir", projectHome);
        properties.setProperty("project.home", projectHome);
        properties.setProperty("sonar.working.directory", projectHome + "/./.sonar");
        if (userCredentials != null) {
            properties.setProperty("sonar.login", userCredentials.getUsername());
            properties.setProperty("sonar.password", PassEncoder.decodeAsString(userCredentials.getPassword()));
        }
        boolean containsSources = configureSourcesAndBinariesProperties(null, project);
        if(containsSources) {
            sourcesCounter++;
        }

        SubprojectProvider subprojectProvider = project.getLookup().lookup(SubprojectProvider.class);
        boolean hasSubprojects = false;
        if (subprojectProvider != null) {
            Set<? extends Project> subprojects = subprojectProvider.getSubprojects();
            hasSubprojects = !subprojects.isEmpty();
            for (Project subproject : subprojects) {
                String module = subproject.getProjectDirectory().getNameExt();
                if (modules.length() > 0) {
                    modules.append(',');
                }
                modules.append(module);
                boolean moduleContainsSources = addModuleProperties(module, subproject);
                if (moduleContainsSources) {
                    sourcesCounter++;
                }
            }
        }
        if (sourcesCounter == 0) {
            throw new SourcesNotFoundException();
        }
        String projectKey = model.getGroupId();
        if (!hasSubprojects) {
            projectKey += ":" + model.getArtifactId();
        }
        properties.setProperty("sonar.projectKey", projectKey);
        if (modules.length() > 0) {
            properties.setProperty("sonar.modules", modules.toString());
        }
        if (outConsumer != null) {
            runner.setStdOut(outConsumer);
        }
        wrapper = new WrapperConsumer(errConsumer);
        runner.setStdErr(wrapper);
        runner.addProperties(properties);
        return runner;
    }

    /**
     * @param project
     * @return True if the project contains sources.
     */
    private boolean configureSourcesAndBinariesProperties(String module, Project project) {
        Sources sources = ProjectUtils.getSources(project);
        SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        if (sourceGroups != null && sourceGroups.length != 0) {
            String sourceProperty = "sonar.sources";
            if (module != null) {
                sourceProperty = module + "." + sourceProperty;
            }
            properties.setProperty(sourceProperty, sourceGroups[0].getRootFolder().getPath());
            URL[] roots = BinaryForSourceQuery.findBinaryRoots(sourceGroups[0].getRootFolder().toURL()).getRoots();
            if (roots.length > 0) {
                File f = Utilities.toFile(roots[0]);
                String binariesProperty = "sonar.binaries";
                if (module != null) {
                    binariesProperty = module + "." + binariesProperty;
                }
                properties.setProperty(binariesProperty, f.getPath());
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean addModuleProperties(String module, Project subproject) throws MvnModelInputException {
        Model submodel = mvnModelFactory.createModel(subproject);
        properties.setProperty(module + ".sonar.projectKey", submodel.getArtifactId());
        properties.setProperty(module + ".sonar.projectName", submodel.getName() != null ? submodel.getName() : submodel.getArtifactId());
        properties.setProperty(module + ".sonar.projectBaseDir", subproject.getProjectDirectory().getPath());
        return configureSourcesAndBinariesProperties(module, subproject);
    }

    public SonarRunnerResult executeRunner(UserCredentials token, ProcessMonitor processMonitor) throws MvnModelInputException {
        Runner runner = createForProject(token, processMonitor);
        try {
            runner.execute();
        } catch (Exception ex) {
            if (wrapper.isUnauthorized()) {
                throw new AuthorizationException();
            } else {
                throw new SonarRunnerException(ex);
            }
        }
        if(processMonitor.stop()) {
            throw new SonarRunnerCancelledException();
        }else{
            File jsonFile = new File(properties.getProperty("sonar.working.directory"), "sonar-report.json");
            if (!jsonFile.exists()) {
                throw new SonarRunnerException();
            } else {
                return new SonarRunnerResult(jsonFile);
            }
        }
    }

    protected static int getMajorVersion(String version) {
        int index = version.indexOf('.');
        if (index != -1) {
            return Integer.parseInt(version.substring(0, index));
        } else {
            throw new IllegalArgumentException("Problem getting major version in " + version);
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
