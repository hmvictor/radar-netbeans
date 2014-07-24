package qubexplorer.ui;

/**
 *
 * @author Victor
 */
public class ProjectNotFoundException extends RuntimeException{
    private final String shortProjectKey;

    public ProjectNotFoundException(String shortProjectKey) {
        this.shortProjectKey = shortProjectKey;
    }

    public String getShortProjectKey() {
        return shortProjectKey;
    }
    
}
