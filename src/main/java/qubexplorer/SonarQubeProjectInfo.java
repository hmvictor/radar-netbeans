package qubexplorer;

import org.netbeans.api.project.Project;

/**
 *
 * @author Victor
 */
public interface SonarQubeProjectInfo {
    
    String getName();
    
    ResourceKey getKey();
    
    String getVersion();
    
    SonarQubeProjectInfo createSubprojectInfo(Project project) throws SonarQubeProjectException;
    
}
