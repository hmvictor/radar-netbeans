package qubexplorer.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author Víctor
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Resource {
    private String name;
    private String key;
    private String version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
}
