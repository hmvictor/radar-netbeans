package qubexplorer.ui;

import org.openide.util.NbPreferences;
import qubexplorer.SonarQube;
import qubexplorer.ui.options.SonarQubeOptionsPanel;

/**
 *
 * @author Victor
 */
public final class SonarQubeFactory {
    
    private SonarQubeFactory() {
        
    }
    
//    public static SonarQube createSonarQubeInstance() {
//        return new SonarQube(NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000"));
//    }
    
}
