package qubexplorer;

import java.util.Properties;
import org.netbeans.api.project.Project;

/**
 *
 * @author Victor
 */
public interface SonarQubeProjectConfiguration {
    
    String getName();
    
    ResourceKey getKey();
    
    String getVersion();

    SonarQubeProjectConfiguration createConfiguration(Project subproject);

    /**
     * Get user defined sonar properties.
     * 
     * @return 
     */
    Properties getProperties();

}
