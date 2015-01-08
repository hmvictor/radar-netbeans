package qubexplorer;

import org.netbeans.api.project.Project;

/**
 *
 * @author Victor
 */
public interface SonarQubeProject {
    
    String getName();
    
    ResourceKey getKey();
    
    String getVersion();
    
    SonarQubeProject createSubprojectInfo(Project project) throws SonarQubeProjectException;
    
}
