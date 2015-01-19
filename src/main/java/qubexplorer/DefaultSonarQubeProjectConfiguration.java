package qubexplorer;


public class DefaultSonarQubeProjectConfiguration implements SonarQubeProjectConfiguration {
    private String name;
    private ResourceKey key;
    private String version;

    public DefaultSonarQubeProjectConfiguration(String name, ResourceKey key, String version) {
        this.name = name;
        this.key = key;
        this.version = version;
    }

    public DefaultSonarQubeProjectConfiguration() {
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
    
}
