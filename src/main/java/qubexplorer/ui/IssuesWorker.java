package qubexplorer.ui;

import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.services.Rule;
import qubexplorer.Authentication;
import qubexplorer.IssueDecorator;
import qubexplorer.Severity;
import qubexplorer.SonarQube;

/**
 *
 * @author Victor
 */
public class IssuesWorker extends SwingWorker<List<IssueDecorator>, Void> {
    private Project project;
    private Severity severity;
    private Rule rule;
    private Authentication auth;
    private ProgressHandle handle;
    private String url;

    public IssuesWorker(Project project, Severity severity, String url) {
        this.project=project;
        this.severity=severity;
        init();
    }
    
    public IssuesWorker(Project project, Rule rule, String url) {
        this.project=project;
        this.rule=rule;
        init();
    }
    
    public void setAuth(Authentication auth) {
        this.auth = auth;
    }
    
    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.switchToIndeterminate();
        handle.start();
    }

    @Override
    protected List<IssueDecorator> doInBackground() throws Exception {
        SonarQube sonarQube = new SonarQube(url);
        if(severity != null) {
            return sonarQube.getIssuesBySeverity(auth, SonarQube.toResource(project), severity.toString());
        }else if(rule != null){
            return sonarQube.getIssuesByRule(auth, SonarQube.toResource(project), rule.getKey());
        }else{
            return sonarQube.getIssuesBySeverity(auth, SonarQube.toResource(project), "any");
        }
    }

    @Override
    protected void done() {
        try {
            SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarTopComponent");
            sonarTopComponent.setIssues(severity == null ? rule: severity, get().toArray(new IssueDecorator[0]));
            sonarTopComponent.open();
            sonarTopComponent.requestVisible();
            sonarTopComponent.setProject(project);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof HttpException) {
                if (((HttpException) ex.getCause()).status() == 401) {
                    if(auth != null) {
                        AuthenticationRepository.getInstance().invalidateAuthentication();
                    }
                    handle.finish();
                    Authentication authentication = AuthenticationRepository.getInstance().getAuthentication();
                    if (authentication != null) {
                        IssuesWorker worker;
                        if(rule != null){
                            worker= new IssuesWorker(project, rule, url);
                        }else{
                            worker= new IssuesWorker(project, severity, url);
                        }
                        worker.setAuth(auth);
                        worker.execute();
                    }
                }
            } else {
                Exceptions.printStackTrace(ex);
            }
        } finally {
            handle.finish();
        }
    }
    
}
