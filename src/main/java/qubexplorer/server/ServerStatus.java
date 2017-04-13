package qubexplorer.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author Víctor
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerStatus {
    private String id;
    private String version;
    private String status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
}
