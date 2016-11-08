package qubexplorer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Victor
 */
public class SonarMvnProject implements SonarQubeProjectConfiguration {

    public static final String PROPERTY_NAME = "sonar.projectName";
    public static final String PROPERTY_VERSION = "sonar.projectVersion";
    public static final String PROPERTY_KEY = "sonar.projectKey";

    private final Model model;

    public SonarMvnProject(Project project) {
        try {
            this.model = createModel(project);
        } catch (MvnModelInputException ex) {
            throw new SonarQubeProjectException(ex);
        }
    }

    @Override
    public String getName() {
        String projectName = model.getProperties().getProperty(PROPERTY_NAME);
        if (projectName != null) {
            return projectName;
        }
        return model.getName() != null ? model.getName() : model.getArtifactId();
    }

    @Override
    public ResourceKey getKey() {
        String projectKey = model.getProperties().getProperty(PROPERTY_KEY);
        if (projectKey != null) {
            return ResourceKey.valueOf(projectKey);
        }
        String groupId = model.getGroupId();
        if (groupId == null && model.getParent() != null) {
            groupId = model.getParent().getGroupId();
        }
        return new ResourceKey(groupId, model.getArtifactId());
    }

    @Override
    public String getVersion() {
        String projectVersion = model.getProperties().getProperty(PROPERTY_VERSION);
        if (projectVersion != null) {
            return projectVersion;
        }
        String version = model.getVersion();
        if (version == null && model.getParent() != null) {
            version = model.getParent().getVersion();
        }
        return version;
    }

    public static Model createModel(Project project) throws MvnModelInputException {
        FileObject pomFile = getPomFileObject(project);
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try (Reader reader = new InputStreamReader(pomFile.getInputStream())) {
            Model model = mavenreader.read(reader);
            model.setPomFile(new File(pomFile.getPath()));
            return model;
        } catch (XmlPullParserException | IOException ex) {
            throw new MvnModelInputException(ex);
        }
    }

    public static boolean isMvnProject(Project project) {
        return getPomFileObject(project) != null;
    }

    public static FileObject getPomFileObject(Project project) {
        return project.getProjectDirectory().getFileObject("pom.xml");
    }

    public static MavenProject createMavenProject(Project project) throws MvnModelInputException {
        return new MavenProject(createModel(project));
    }

    public static File getOutputDirectory(Project project) throws MvnModelInputException {
        MavenProject mavenProject = SonarMvnProject.createMavenProject(project);
        Build build = mavenProject.getBuild();
        String path = null;
        if (build != null) {
            path = build.getDirectory();
        }
        File outputDirectory;
        if (path != null) {
            outputDirectory = FileUtil.normalizeFile(new File(path));
        } else {
            outputDirectory = new File(project.getProjectDirectory().getPath(), "target");
        }
        return outputDirectory;
    }

    @Override
    public SonarQubeProjectConfiguration createConfiguration(Project subproject) {
        return new SonarMvnProject(subproject);
    }

    @Override
    public Properties getProperties() {
        Properties properties = new Properties();
        for (Map.Entry<Object, Object> entry : model.getProperties().entrySet()) {
            if (entry.getKey().toString().startsWith("sonar.")) {
                properties.put(entry.getKey(), entry.getValue());
            }
        }
        return properties;
    }

}
