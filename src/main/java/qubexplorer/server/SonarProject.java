package qubexplorer.server;

import qubexplorer.ResourceKey;

/**
 *
 * @author Victor
 */
public class SonarProject {
    private final String name;
    private final ResourceKey key;

    public SonarProject(String name, ResourceKey key) {
        this.name = name;
        this.key = key;
    }
    

    public String getName() {
        return name;
    }

    public ResourceKey getKey() {
        return key;
    }
    
}
