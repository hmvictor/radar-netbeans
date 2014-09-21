package qubexplorer.server;

/**
 *
 * @author Victor
 */
public class SonarProject {
    private final String key;
    private final String name;

    public SonarProject(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }
    
}
