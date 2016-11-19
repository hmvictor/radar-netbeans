package qubexplorer.ui.task;

import org.openide.util.Exceptions;
import qubexplorer.UserCredentials;
import qubexplorer.ui.ProjectContext;

/**
 *
 * @author Victor
 */
public abstract class Task<T> {
    private final ProjectContext projectContext;
    private final String serverUrl;
    private UserCredentials userCredentials;
    private boolean retryIfNoAuthorization=true;

    public Task(ProjectContext projectContext, String serverUrl) {
        this.projectContext = projectContext;
        this.serverUrl=serverUrl;
    }

    public void setRetryIfNoAuthorization(boolean retryIfNoAuthorization) {
        this.retryIfNoAuthorization = retryIfNoAuthorization;
    }

    public boolean isRetryIfNoAuthorization() {
        return retryIfNoAuthorization;
    }
    
    public void setUserCredentials(UserCredentials userCredentials) {
        this.userCredentials = userCredentials;
    }

    public UserCredentials getUserCredentials() {
        return userCredentials;
    }

    public ProjectContext getProjectContext() {
        return projectContext;
    }

    public String getServerUrl() {
        return serverUrl;
    }
    
    public abstract T execute() throws TaskExecutionException;

    protected void reset() {

    }

    protected void init() {

    }

    protected void success(T result) {

    }

    protected void fail(Throwable ex) {
        Exceptions.printStackTrace(ex);
    }

    protected void completed() {

    }

    protected void destroy() {
        
    }

}
