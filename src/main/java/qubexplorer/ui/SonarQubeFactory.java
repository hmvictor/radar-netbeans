package qubexplorer.ui;

import org.openide.util.NbPreferences;
import qubexplorer.server.SonarQube;

/**
 *
 * @author Victor
 */
public final class SonarQubeFactory {
    
    private SonarQubeFactory(){
    }
    
    public static SonarQube createForDefaultServerUrl(){
        String serverUrl=NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000");
        return new SonarQube(serverUrl);
    }
    
}
