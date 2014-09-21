package qubexplorer.ui.task;

import qubexplorer.AuthenticationToken;
import qubexplorer.ui.ProjectContext;

/**
 *
 * @author Victor
 */
public abstract class Task<T> {
    private AuthenticationToken token;
    private final ProjectContext projectContext;
    private final String serverUrl;

    public Task(ProjectContext projectContext, String serverUrl) {
        this.projectContext = projectContext;
        this.serverUrl=serverUrl;
    }
    
    public void setToken(AuthenticationToken token) {
        this.token = token;
    }

    public AuthenticationToken getToken() {
        return token;
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
