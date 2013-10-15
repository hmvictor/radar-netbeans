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

    public IssuesWorker(Project project, Severity severity) {
        this.project=project;
        this.severity=severity;
        init();
    }
    
    public IssuesWorker(Project project, Rule rule) {
        this.project=project;
        this.rule=rule;
        init();
    }
    
    public IssuesWorker(Authentication auth, Project project, Severity severity) {
        this.project=project;
        this.severity=severity;
        this.auth=auth;
        init();
    }
    
    public IssuesWorker(Authentication auth, Project project, Rule rule) {
        this.auth=auth;
        this.project=project;
        this.rule=rule;
        init();
    }

    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.switchToIndeterminate();
        handle.start();
    }

    @Override
    protected List<IssueDecorator> doInBackground() throws Exception {
        if(severity != null) {
            return SonarQubeFactory.createSonarQubeInstance().getIssuesBySeverity(auth, SonarQube.toResource(project), severity.toString());
        }else if(rule != null){
            return SonarQubeFactory.createSonarQubeInstance().getIssuesByRule(auth, SonarQube.toResource(project), rule.getKey());
        }else{
            return SonarQubeFactory.createSonarQubeInstance().getIssuesBySeverity(auth, SonarQube.toResource(project), "any");
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
                            worker= new IssuesWorker(authentication, project, rule);
                        }else{
                            worker= new IssuesWorker(authentication, project, severity);
                        }
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
