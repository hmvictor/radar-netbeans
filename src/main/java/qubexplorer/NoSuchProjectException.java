package qubexplorer;

/**
 *
 * @author Victor
 */
public class NoSuchProjectException extends RuntimeException{
    private final ResourceKey projectKey;

    public NoSuchProjectException(ResourceKey projectKey) {
        this.projectKey = projectKey;
    }

    public ResourceKey getProjectKey() {
        return projectKey;
    }
    
}
