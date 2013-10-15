package qubexplorer.ui;

import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.services.Rule;
import qubexplorer.Authentication;
import qubexplorer.IssueDecorator;
import qubexplorer.Severity;
import qubexplorer.SonarQube;
import qubexplorer.ui.options.SonarQubeOptionsPanel;

/**
 *
 * @author Victor
 */
public class IssuesWorker extends SwingWorker<List<IssueDecorator>, Void> {
    private final String address;
    private Project project;
    private Severity severity;
    private Rule rule;
    private Authentication auth;
    private ProgressHandle handle;

    public IssuesWorker(Project project, Severity severity) {
        this.project=project;
        this.severity=severity;
        this.address = NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000");
        init();
    }
    
    public IssuesWorker(Project project, Rule rule) {
        this.project=project;
        this.rule=rule;
        this.address = NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000");
        init();
    }
    
    public IssuesWorker(Authentication auth, Project project, Severity severity) {
        this.project=project;
        this.severity=severity;
        this.auth=auth;
        this.address = NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000");
        init();
    }
    
    public IssuesWorker(Authentication auth, Project project, Rule rule) {
        this.auth=auth;
        this.project=project;
        this.rule=rule;
        this.address = NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000");
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
            return new SonarQube(address).getIssues(auth, SonarQube.toResource(project), severity.toString());
        }else if(rule != null){
            return new SonarQube(address).getIssuesByRule(auth, SonarQube.toResource(project), rule.getKey());
        }else{
            return new SonarQube(address).getIssues(auth, SonarQube.toResource(project), "any");
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
