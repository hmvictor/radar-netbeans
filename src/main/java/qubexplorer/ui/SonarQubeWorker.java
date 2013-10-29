package qubexplorer.ui;

import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;
import qubexplorer.Authentication;
import qubexplorer.AuthorizationException;

/**
 *
 * @author Victor
 */
public abstract class SonarQubeWorker<R,P> extends SwingWorker<R, P>{
    private String serverUrl;
    private String resourceKey;
    private Authentication authentication;
    private SwingWorker<R, P> scheduledWorker;

    public SonarQubeWorker(String serverUrl, String resourceKey) {
        this.serverUrl = serverUrl;
        this.resourceKey = resourceKey;
        authentication=AuthenticationRepository.getInstance().getAuthentication(serverUrl, resourceKey);
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getResourceKey() {
        return resourceKey;
    }
    
    @Override
    protected final void done() {
        try{
            R result = get();
            success(result);
            if(authentication != null) {
                AuthenticationRepository.getInstance().saveAuthentication(serverUrl, resourceKey, authentication);
            }
        }catch(ExecutionException ex) {
            Throwable cause = ex.getCause();
            if(cause instanceof AuthorizationException) {
                AuthenticationRepository repo=AuthenticationRepository.getInstance();
                Authentication auth=repo.getAuthentication(serverUrl, resourceKey);
                if(auth == null) {
                    auth=AuthDialog.showAuthDialog(WindowManager.getDefault().getMainWindow());
                }
                if(auth != null) {
                    SonarQubeWorker copy=createCopy();
                    copy.setAuthentication(auth);
                    scheduleWorker(copy);
                }
            }else{
                error(cause);
            }
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } finally{
            finished();
            if(scheduledWorker != null) {
                scheduledWorker.execute();
            }
        }
    }
    
    protected void success(R result) {
    }

    protected void error(Throwable cause) {
        Exceptions.printStackTrace(cause);
    }

    protected void finished() {
    }

    protected abstract SonarQubeWorker createCopy();

    protected void scheduleWorker(SonarQubeWorker copy) {
        scheduledWorker=copy;
    }

}
