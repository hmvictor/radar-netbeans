package qubexplorer.ui;

import org.openide.util.NbPreferences;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.options.SonarQubeOptionsPanel;

/**
 *
 * @author Victor
 */
public class SonarQubeFactory {
    
    private SonarQubeFactory(){}
    
    public static SonarQube createForDefaultServerUrl(){
        String serverUrl=NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000");
        return new SonarQube(serverUrl);
    }
    
}
