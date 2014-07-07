package qubexplorer.runner;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.queries.FileEncodingQuery;
import org.sonar.runner.api.EmbeddedRunner;
import org.sonar.runner.api.Runner;
import qubexplorer.MvnModelFactory;

/**
 *
 * @author Victor
 */
public class SonarRunnerProccess {
    private static final Logger logger = Logger.getLogger(SonarRunnerProccess.class.getName());
    
    private final Project project;
    private final String sonarUrl;
    private AnalysisMode analysisMode=AnalysisMode.INCREMENTAL;
    
    public enum AnalysisMode {
        INCREMENTAL,
        PREVIEW;
    }
    
    /**
     * This state is modified while running.
     */
    private final MvnModelFactory mvnModelFactory=new MvnModelFactory();
    private String projectHome;
    private final StringBuilder modules=new StringBuilder();
    public Properties properties=new Properties();

    public SonarRunnerProccess(String sonarUrl, Project project) {
        this.sonarUrl=sonarUrl;
        this.project = project;
    }

    public AnalysisMode getAnalysisMode() {
        return analysisMode;
    }

    public void setAnalysisMode(AnalysisMode analysisMode) {
        Objects.requireNonNull(analysisMode, "analysisMode is null");
        this.analysisMode = analysisMode;
    }
    
    protected Runner createForProject() throws IOException, XmlPullParserException {
        EmbeddedRunner runner=EmbeddedRunner.create();
        projectHome=project.getProjectDirectory().getPath();
        Model model = mvnModelFactory.createModel(project);
        properties.put("sonar.projectKey", model.getArtifactId()+":"+model.getGroupId());
        properties.put("sonar.projectName", model.getName());
        properties.put("sonar.projectVersion", model.getVersion());
        properties.put("sonar.sourceEncoding", FileEncodingQuery.getEncoding(project.getProjectDirectory()).displayName());
        properties.put("sonar.host.url", sonarUrl);
        properties.put("sonar.analysis.mode", analysisMode.toString().toLowerCase());
        properties.put("sonar.dryRun", "true");
        properties.put("sonar.projectDir", projectHome);
        Sources sources = ProjectUtils.getSources(project);
        SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        if(sourceGroups == null || sourceGroups.length == 0){
            throw new IllegalStateException("No sources for this project");
        }
        //TODO handle more the one source group
        properties.put("sonar.sources", sourceGroups[0].getRootFolder().getPath());
        properties.put("project.home", projectHome);
        properties.put("sonar.projectBaseDir", projectHome);
        properties.put("sonar.working.directory", projectHome+"/./.sonar");
        for(String module:model.getModules()) {
            proccessModule(module);
        }
        if(modules.length() > 0){
            properties.put("sonar.modules", modules);
        }
        runner.addProperties(properties);
        return runner;
    }
    
    private void proccessModule(String  module) throws IOException, XmlPullParserException {
        if(modules.length() > 0) { 
            modules.append(module);
        }
        Model model=mvnModelFactory.createModel(new File(projectHome, module));
        properties.put(module+".sonar.projectName", model.getName());
        for(String submodule: model.getModules()) {
            proccessModule(submodule);
        }
    }
    
    public SonarRunnerResult executeRunner() throws IOException, XmlPullParserException {
        Runner runner = createForProject();
        try{
            runner.execute();
        }catch(Exception ex) {
            logger.log(Level.WARNING, ex.toString(), ex);
            throw new SonarRunnerException();
        }
        File jsonFile=new File(properties.getProperty("sonar.working.directory"), "sonar-report.json");
        if(!jsonFile.exists()) {
            throw new SonarRunnerException();
        }else{
            return new SonarRunnerResult(jsonFile);
        }
    }
    
}
