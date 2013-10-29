package qubexplorer.ui;

import java.util.HashMap;
import java.util.Map;
import org.openide.windows.WindowManager;
import qubexplorer.Authentication;

/**
 *
 * @author Victor
 */
public class AuthenticationRepository {
    private Authentication authentication;
    private Map<String, Map<String, Authentication>> cache=new HashMap<>();
    
    public void invalidateAuthentication(){
        authentication=null;
    }
    
    public Authentication getAuthentication() {
        if(authentication == null) {
            authentication=AuthDialog.showAuthDialog(WindowManager.getDefault().getMainWindow());
        }
        return authentication;
    }
    
    public Authentication getAuthentication(String serverUrl, String resourceKey) {
        if(cache.containsKey(serverUrl)) {
            if(cache.get(serverUrl).containsKey(resourceKey)) {
                return cache.get(serverUrl).get(resourceKey);
            }else{
                return cache.get(serverUrl).get(null);
            }
        }else{
            return null;
        }
    }
    
    public void saveAuthentication(String serverUrl, String resourceKey, Authentication authentication) {
        if(!cache.containsKey(serverUrl)) {
            cache.put(serverUrl, new HashMap<String, Authentication>());
        }
        cache.get(serverUrl).put(null, authentication);
        if(resourceKey != null) {
            cache.get(serverUrl).put(resourceKey, authentication);
        }
    }
    
    private static AuthenticationRepository repository;
    
    public static synchronized AuthenticationRepository getInstance(){
        if(repository == null) {
            repository=new AuthenticationRepository();
        }
        return repository;
    }

}
