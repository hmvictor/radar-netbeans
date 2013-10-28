package qubexplorer;

/**
 *
 * @author Victor
 */
public class NoSuchProjectException extends RuntimeException{
    private String projectKey;

    public NoSuchProjectException(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getProjectKey() {
        return projectKey;
    }
    
}
