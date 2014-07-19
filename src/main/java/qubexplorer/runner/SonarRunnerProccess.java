package qubexplorer.runner;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
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
import org.netbeans.spi.project.SubprojectProvider;
import org.openide.util.Lookup;
import org.sonar.runner.api.ForkedRunner;
import org.sonar.runner.api.PrintStreamConsumer;
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
    private PrintStreamConsumer outConsumer;
    private PrintStreamConsumer errConsumer;
    
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
    
    protected Runner createForProject() throws IOException, XmlPullParserException {
        ForkedRunner runner=ForkedRunner.create();
        projectHome=project.getProjectDirectory().getPath();
        Model model = mvnModelFactory.createModel(project);
        properties.setProperty("sonar.projectName", model.getName() != null? model.getName(): model.getArtifactId());
        properties.setProperty("sonar.projectVersion", model.getVersion());
        properties.setProperty("sonar.sourceEncoding", FileEncodingQuery.getEncoding(project.getProjectDirectory()).displayName());
        properties.setProperty("sonar.host.url", sonarUrl);
        properties.setProperty("sonar.analysis.mode", analysisMode.toString().toLowerCase());
        properties.setProperty("sonar.dryRun", "true");
        properties.setProperty("sonar.projectDir", projectHome);
        properties.setProperty("project.home", projectHome);
        properties.setProperty("sonar.projectBaseDir", projectHome);
        properties.setProperty("sonar.working.directory", projectHome+"/./.sonar");
        Sources sources = ProjectUtils.getSources(project);
        SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        if(sourceGroups != null && sourceGroups.length != 0){
            properties.setProperty("sonar.sources", sourceGroups[0].getRootFolder().getPath());
        }
        SubprojectProvider subprojectProvider = project.getLookup().lookup(SubprojectProvider.class);
        boolean hasSubprojects=false;
        if(subprojectProvider != null) {
            Set<? extends Project> subprojects = subprojectProvider.getSubprojects();
            hasSubprojects=!subprojects.isEmpty();
            for (Project subproject : subprojects) {
                String module=subproject.getProjectDirectory().getName();
                if(modules.length() > 0) { 
                    modules.append(',');
                }
                modules.append(module);
                Model submodel = mvnModelFactory.createModel(subproject);
                properties.setProperty(module+".sonar.projectKey", submodel.getArtifactId());
                properties.setProperty(module+".sonar.projectName", submodel.getName() != null ?  submodel.getName(): submodel.getArtifactId());
                properties.setProperty(module+".sonar.projectBaseDir", subproject.getProjectDirectory().getPath());
                Sources src = ProjectUtils.getSources(subproject);
                SourceGroup[] srcGroups = src.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
                if(srcGroups != null && srcGroups.length != 0){
                    properties.setProperty(module+".sonar.sources", srcGroups[0].getRootFolder().getPath());
                }
            }
        }
        if(hasSubprojects) {
            properties.setProperty("sonar.projectKey", model.getGroupId());
        }else{
            properties.setProperty("sonar.projectKey", model.getGroupId()+":"+model.getArtifactId());
        }
//        for(String module:model.getModules()) {
//            proccessModule(module);
//        }
        if(modules.length() > 0){
            properties.setProperty("sonar.modules", modules.toString());
        }
        if(outConsumer != null) {
            runner.setStdOut(outConsumer);
        }
        if(errConsumer != null) {
            runner.setStdErr(errConsumer);
        }
        //TODO
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            System.out.printf("%s : %s%n", entry.getKey(), entry.getValue());
        }
        runner.addProperties(properties);
        return runner;
    }
    
    private void proccessModule(String  module) throws IOException, XmlPullParserException {
        if(modules.length() > 0) { 
            modules.append(module);
        }
        Model model=mvnModelFactory.createModel(new File(projectHome, module));
        if(model.getModules().isEmpty()) {
            String src="src/main/java";
            if(model.getBuild() != null && model.getBuild().getSourceDirectory() != null) {
                src=model.getBuild().getSourceDirectory();
            }
            properties.setProperty(module+".sonar.sources", src);
        }
        properties.setProperty(module+".sonar.projectName", model.getName() != null ?  model.getName(): model.getArtifactId());
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
