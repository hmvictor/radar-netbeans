package qubexplorer.runner;

import java.io.File;
import java.net.URL;
import java.util.Map;
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
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.server.Version;
import qubexplorer.ui.ProjectContext;

/**
 *
 * @author Victor
 */
public class Module {

    private final ProjectContext projectContext;
    private final ProjectContext parentProjectContext;

    private Module(ProjectContext parentProjectContext, ProjectContext projectContext) {
        this.parentProjectContext = parentProjectContext;
        this.projectContext = projectContext;
    }

    public boolean isRootProject() {
        return parentProjectContext == null;
    }

    public String getName() {
        return isRootProject() ? null : projectContext.getProject().getProjectDirectory().getNameExt();
    }

    public boolean containsSources() {
        return getMainSourceGroup() != null;
    }

    public SourceGroup getMainSourceGroup() {
        Sources sources = ProjectUtils.getSources(projectContext.getProject());
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
        Properties projectProperties = projectContext.getConfiguration().getProperties();
        properties.putAll(projectProperties);
    }

    private String getPropertyName(String property) {
        if (!isRootProject()) {
            return getName() + "." + property;
        }
        return property;
    }

    protected void configureSourcesAndBinariesProperties(Version sonarQubeVersion, Properties properties) {
        SourceGroup mainSourceGroup = getMainSourceGroup();
        if (mainSourceGroup != null) {
            String sourcePath = mainSourceGroup.getRootFolder().getPath();
            if (SonarMvnProject.isMvnProject(projectContext.getProject()) && sonarQubeVersion.compareTo(4, 5) >= 0) {
                sourcePath = "pom.xml," + sourcePath;
            }
            ClassPath classPath = ClassPath.getClassPath(projectContext.getProject().getProjectDirectory(), ClassPath.COMPILE);
            if (classPath != null) {
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
                if (testsDir.exists()) {
                    properties.setProperty(getPropertyName("sonar.tests"), testsDir.getPath());
                }
            }
        }
    }

    public void addModuleProperties(Version sonarQubeVersion, Properties properties) throws MvnModelInputException {
        SonarQubeProjectConfiguration projectConfiguration = projectContext.getConfiguration();
        configureSourcesAndBinariesProperties(sonarQubeVersion, properties);
        properties.setProperty(getPropertyName("sonar.projectName"), projectConfiguration.getName());
        properties.setProperty(getPropertyName("sonar.projectKey"), projectConfiguration.getKey().toString());
        if (containsSources()) {
            properties.setProperty(getPropertyName("sonar.projectBaseDir"), projectContext.getProject().getProjectDirectory().getPath());
        }
    }

    public Module createSubmodule(Project subproject) {
        return new Module(projectContext, new ProjectContext(subproject, projectContext.getConfiguration().createConfiguration(subproject)));
    }

    public static Module createMainModule(ProjectContext projectContext) {
        return new Module(null, projectContext);
    }

    private static String getLibrariesPath(ClassPath classPath) {
        StringBuilder librariesPath = new StringBuilder();
        for (FileObject root : classPath.getRoots()) {
            if (librariesPath.length() > 0) {
                librariesPath.append(',');
            }
            FileObject archiveFile = FileUtil.getArchiveFile(root);
            if (archiveFile != null) {
                librariesPath.append(archiveFile.getPath());
            }
        }
        return librariesPath.toString();
    }

}
