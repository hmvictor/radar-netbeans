package qubexplorer;

import java.util.Properties;
import org.netbeans.api.project.Project;


public class GenericSonarQubeProjectConfiguration implements SonarQubeProjectConfiguration {
    private String name;
    private ResourceKey key;
    private String version;

    public GenericSonarQubeProjectConfiguration(String name, ResourceKey key, String version) {
        this.name = name;
        this.key = key;
        this.version = version;
    }

    public GenericSonarQubeProjectConfiguration() {
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setKey(ResourceKey key) {
        this.key = key;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public ResourceKey getKey() {
        return key;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public SonarQubeProjectConfiguration createConfiguration(Project subproject) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Properties getProperties() {
        throw new UnsupportedOperationException();
    }
    
}
