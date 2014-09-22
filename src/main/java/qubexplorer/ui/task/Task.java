package qubexplorer.ui.task;

import qubexplorer.UserCredentials;
import qubexplorer.ui.ProjectContext;

/**
 *
 * @author Victor
 */
public abstract class Task<T> {
    private UserCredentials userCredentials;
    private final ProjectContext projectContext;
    private final String serverUrl;

    public Task(ProjectContext projectContext, String serverUrl) {
        this.projectContext = projectContext;
        this.serverUrl=serverUrl;
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
    
    public abstract T execute() throws Exception;

    protected void reset() {

    }

    protected void init() {

    }

    protected void success(T result) {

    }

    protected void fail(Throwable ex) {

    }

    protected void completed() {

    }

    protected void destroy() {
        
    }

}
