package qubexplorer;

import org.openide.windows.WindowManager;
import qubexplorer.ui.AuthDialog;

/**
 *
 * @author Victor
 */
public class AuthenticationRepository {
    private Authentication authentication;
    
    public void invalidateAuthentication(){
        authentication=null;
    }
    
    public Authentication getAuthentication() {
        if(authentication == null) {
            authentication=AuthDialog.showAuthDialog(WindowManager.getDefault().getMainWindow());
        }
        return authentication;
    }
    
    private static AuthenticationRepository repository;
    
    public static synchronized AuthenticationRepository getInstance(){
        if(repository == null) {
            repository=new AuthenticationRepository();
        }
        return repository;
    }
    
}
