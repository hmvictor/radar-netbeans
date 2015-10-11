package qubexplorer.runner;

import java.io.File;
import java.net.URL;
import java.util.Properties;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import org.netbeans.api.java.queries.UnitTestForSourceQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;
import qubexplorer.MvnModelInputException;
import qubexplorer.SonarMvnProject;
import qubexplorer.SonarQubeProjectBuilder;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.server.Version;

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
    
    public void loadExternalProperties(Properties properties) {
        if(SonarMvnProject.isMvnProject(project)){
            /* Read from pom file */
            throw new UnsupportedOperationException("Not yet implemented");
        }else{
            /* Read from properties or from configuration? */
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }
    
    private String getPropertyName(String property) {
        if (!mainModule) {
            return name + "." + property;
        }
        return property;
    }
    
    protected void configureSourcesAndBinariesProperties(Version sonarQubeVersion, Properties properties) {
        SourceGroup mainSourceGroup=getMainSourceGroup();
        if (mainSourceGroup != null) {
            String sourcePath=mainSourceGroup.getRootFolder().getPath();
            if(SonarMvnProject.isMvnProject(project) && sonarQubeVersion.compareTo(4, 5) >= 0){
                sourcePath="pom.xml,"+sourcePath;
            }
            ClassPath classPath = ClassPath.getClassPath(project.getProjectDirectory(), ClassPath.COMPILE);
            if(classPath != null){
                properties.setProperty(getPropertyName("sonar.java.libraries"), getLibrariesPath(classPath));
            }
            properties.setProperty(getPropertyName("sonar.sources"), sourcePath);
            URL[] roots = BinaryForSourceQuery.findBinaryRoots(mainSourceGroup.getRootFolder().toURL()).getRoots();
            if (roots.length > 0) {
                properties.setProperty(getPropertyName("sonar.java.binaries"), Utilities.toFile(roots[0]).getPath());
            }
            URL[] testSources = UnitTestForSourceQuery.findUnitTests(mainSourceGroup.getRootFolder());
            if (testSources != null && testSources.length != 0) {
                File testsDir = FileUtil.archiveOrDirForURL(testSources[0]);
                if(testsDir.exists()){
                    properties.setProperty(getPropertyName("sonar.tests"), testsDir.getPath());
                }
            }
        }
    }
    
    public void addModuleProperties(Version sonarQubeVersion, Properties properties) throws MvnModelInputException {
        SonarQubeProjectConfiguration subprojectInfo = SonarQubeProjectBuilder.getDefaultConfiguration(project);
        configureSourcesAndBinariesProperties(sonarQubeVersion, properties);
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
    
    private static String getLibrariesPath(ClassPath classPath) {
        StringBuilder librariesPath=new StringBuilder();
        for (FileObject root : classPath.getRoots()) {
            if(librariesPath.length() > 0){
                librariesPath.append(',');
            }
            FileObject archiveFile = FileUtil.getArchiveFile(root);
            if(archiveFile != null){
                librariesPath.append(archiveFile.getPath());
            }
        }
        return librariesPath.toString();
    }

}
