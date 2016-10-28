package qubexplorer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.openide.filesystems.FileObject;

public class PlainDirectoryProject implements SonarQubeProjectConfiguration {

    private final Project project;
    private Properties properties;

    public PlainDirectoryProject(Project project) throws IOException {
        this.project = project;
        loadProjectProperties();
    }
    
    private void loadProjectProperties() throws IOException {
        properties = new Properties();
        FileObject fileObject = project.getProjectDirectory().getFileObject("sonar.properties");
        if (fileObject != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileObject.getInputStream(), StandardCharsets.UTF_8))) {
                properties.load(reader);
            }
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return properties.getProperty(SonarMvnProject.PROPERTY_NAME, ProjectUtils.getInformation(project).getName());
    }

    @Override
    public ResourceKey getKey() {
        String propertyValue = properties.getProperty(SonarMvnProject.PROPERTY_KEY);
        if (propertyValue != null) {
            return ResourceKey.valueOf(propertyValue);
        } else {
//            return new ResourceKey(parentConfig.getKey().getPart(0), getName());
            return new ResourceKey(getName());
        }
    }

    @Override
    public String getVersion() {
        return properties.getProperty(SonarMvnProject.PROPERTY_VERSION, "1.0");
    }

}
