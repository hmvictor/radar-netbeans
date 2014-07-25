package qubexplorer.ui;

import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;
import qubexplorer.AuthenticationToken;
import qubexplorer.AuthorizationException;

/**
 *
 * @author Victor
 */
public abstract class SonarQubeWorker<R,P> extends UITask<R, P>{
    private String serverUrl;
    private final String projectKey;
    private AuthenticationToken authentication;
    private SwingWorker<R, P> scheduledWorker;

    public SonarQubeWorker(String projectKey) {
        this.projectKey = projectKey;
    }

    protected void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        authentication=AuthenticationRepository.getInstance().getAuthentication(serverUrl, projectKey);
    }
    
    public void setAuthentication(AuthenticationToken authentication) {
        this.authentication = authentication;
    }

    public AuthenticationToken getAuthentication() {
        return authentication;
    }

    public String getProjectKey() {
        return projectKey;
    }
    
    @Override
    protected final void done() {
        try {
            R result = get();
            success(result);
            if (authentication != null) {
                AuthenticationRepository.getInstance().saveAuthentication(serverUrl, projectKey, authentication);
            }
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof AuthorizationException) {
                AuthenticationRepository repo = AuthenticationRepository.getInstance();
                AuthenticationToken auth = repo.getAuthentication(serverUrl, projectKey);
                if (auth == null) {
                    auth = AuthDialog.showAuthDialog(WindowManager.getDefault().getMainWindow());
                }
                if (auth != null) {
                    SonarQubeWorker copy = createCopy();
                    copy.setAuthentication(auth);
                    scheduleWorker(copy);
                }
            } else {
                error(cause);
            }
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            finished();
            if (scheduledWorker != null) {
                scheduledWorker.execute();
            }
        }
    }
    
    protected abstract SonarQubeWorker createCopy();

    protected void scheduleWorker(SonarQubeWorker copy) {
        scheduledWorker=copy;
    }

}
