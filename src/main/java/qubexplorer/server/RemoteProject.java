package qubexplorer.server;

import org.netbeans.api.project.Project;
import qubexplorer.ResourceKey;
import qubexplorer.SonarQubeProjectException;
import qubexplorer.SonarQubeProject;

/**
 *
 * @author Victor
 */
public class RemoteProject implements SonarQubeProject{
    private final String name;
    private final ResourceKey key;
    private final String version;

    public RemoteProject(String name, ResourceKey key, String version) {
        this.name = name;
        this.key = key;
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
    public SonarQubeProject createSubprojectInfo(Project project) throws SonarQubeProjectException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
