package qubexplorer.runner;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import org.netbeans.api.java.queries.UnitTestForSourceQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;
import qubexplorer.MvnModelInputException;
import qubexplorer.SonarMvnProject;
import qubexplorer.SonarQubeProjectBuilder;
import qubexplorer.SonarQubeProjectConfiguration;

/**
 *
 * @author Victor
 */
public class Module {
    private final String name;
    private final Project project;
    private final boolean mainModule;

    public Module(String name, Project project, boolean mainModule) {
        this.name = name;
        this.project = project;
        this.mainModule = mainModule;
    }
    
    public String getName() {
        return name;
    }

    public Project getProject() {
        return project;
    }

    public boolean isMainModule() {
        return mainModule;
    }

    public String getPropertyName(String property) {
        if (!mainModule) {
            return name + "." + property;
        }
        return property;
    }

    public boolean containsSources() {
        return getMainSourceGroup() != null;
    }

    public SourceGroup getMainSourceGroup() {
        Sources sources = ProjectUtils.getSources(project);
        SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        SourceGroup mainSourceGroup = null;
        if (sourceGroups != null && sourceGroups.length != 0) {
            for (SourceGroup sGroup : sourceGroups) {
                URL[] sourcesForUnitTest = UnitTestForSourceQuery.findSources(sGroup.getRootFolder());
                if (sourcesForUnitTest == null || sourcesForUnitTest.length == 0) {
                    mainSourceGroup = sGroup;
                    break;
                }
            }
        }
        return mainSourceGroup;
    }
    
    
    boolean configureSourcesAndBinariesProperties(Properties properties) {
        SourceGroup mainSourceGroup=getMainSourceGroup();
        if (mainSourceGroup != null) {
            String sourcePath=mainSourceGroup.getRootFolder().getPath();
            boolean isMvnProject = SonarMvnProject.isMvnProject(project);
            if(isMvnProject) {
                sourcePath="pom.xml,"+sourcePath;
            }
            properties.setProperty(getPropertyName("sonar.sources"), sourcePath);
            if(isMvnProject){
//                try {
                    NbMavenProject nbMavenProject = project.getLookup().lookup(NbMavenProject.class);
                    MavenProject mavenProject = nbMavenProject.getMavenProject();
                    Set<Artifact> artifacts = mavenProject.getArtifacts();
                    StringBuilder librariesPath=new StringBuilder();
                    for (Artifact artifact : artifacts) {
                            if(librariesPath.length() > 0){
                                librariesPath.append(',');
                            }
                        librariesPath.append(artifact.getFile().getAbsolutePath());
                    }
//                  properties.setProperty(getPropertyName("sonar.java.libraries"), projectHome + "/target/nbm/netbeans/extra/modules/ext/**/*.jar,C:/Users/Victor/.m2/repository/org/netbeans/**/*.jar");
                    properties.setProperty(getPropertyName("sonar.java.libraries"), librariesPath.toString());
//                } catch (MvnModelInputException ex) {
//                    throw new SonarRunnerException(ex);
//                }
            }
            URL[] roots = BinaryForSourceQuery.findBinaryRoots(mainSourceGroup.getRootFolder().toURL()).getRoots();
            if (roots.length > 0) {
                File f = Utilities.toFile(roots[0]);
                properties.setProperty(getPropertyName("sonar.java.binaries"), f.getPath());
            }
            URL[] testSources = UnitTestForSourceQuery.findUnitTests(mainSourceGroup.getRootFolder());
            if (testSources != null && testSources.length != 0) {
                File testsDir = FileUtil.archiveOrDirForURL(testSources[0]);
                if(testsDir.exists()){
                    properties.setProperty(getPropertyName("sonar.tests"), testsDir.getPath());
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public void addModuleProperties(Properties properties) throws MvnModelInputException {
        SonarQubeProjectConfiguration subprojectInfo = SonarQubeProjectBuilder.getDefaultConfiguration(project);
        configureSourcesAndBinariesProperties(properties);
        if (containsSources()) {
            properties.setProperty(getPropertyName("sonar.projectName"), subprojectInfo.getName());
            assert subprojectInfo.getKey().getPartsCount() == 2;
            properties.setProperty(getPropertyName("sonar.projectKey"), subprojectInfo.getKey().getPart(1));
            properties.setProperty(getPropertyName("sonar.projectBaseDir"), project.getProjectDirectory().getPath());
        }
    }

    public static Module createMainModule(Project project) {
        return new Module(null, project, true);
    }

    public static Module createSubmodule(Project submoduleProject) {
        return new Module(submoduleProject.getProjectDirectory().getNameExt(), submoduleProject, false);
    }

}
