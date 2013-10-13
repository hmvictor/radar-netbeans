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
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.services.Rule;
import qubexplorer.Authentication;
import qubexplorer.Severity;
import qubexplorer.SonarQube;
import qubexplorer.ui.options.SonarQubePanel;

/**
 *
 * @author Victor
 */
public class IssuesWorker extends SwingWorker<List<Issue>, Void> {
    private final String address;
    private Project project;
    private Severity severity;
    private Rule rule;
    private Authentication auth;
    private ProgressHandle handle;

    public IssuesWorker(Project project, Severity severity) {
        this.project=project;
        this.severity=severity;
        this.address = NbPreferences.forModule(SonarQubePanel.class).get("address", "http://localhost:9000");
        init();
    }
    
    public IssuesWorker(Project project, Rule rule) {
        this.project=project;
        this.rule=rule;
        this.address = NbPreferences.forModule(SonarQubePanel.class).get("address", "http://localhost:9000");
        init();
    }
    
    public IssuesWorker(Authentication auth, Project project, Severity severity) {
        this.project=project;
        this.severity=severity;
        this.auth=auth;
        this.address = NbPreferences.forModule(SonarQubePanel.class).get("address", "http://localhost:9000");
        init();
    }
    
    public IssuesWorker(Authentication auth, Project project, Rule rule) {
        this.auth=auth;
        this.project=project;
        this.rule=rule;
        this.address = NbPreferences.forModule(SonarQubePanel.class).get("address", "http://localhost:9000");
        init();
    }

//    public IssuesWorker(Authentication auth, String address, String key, String severity, SonarTopComponent sonarTopComponent) {
//        this.address = address;
//        this.key = key;
//        this.severity = severity;
//        this.sonarTopComponent = sonarTopComponent;
//        this.auth = auth;
//        init();
//    }

    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.switchToIndeterminate();
        handle.start();
    }

    @Override
    protected List<Issue> doInBackground() throws Exception {
        if(severity != null) {
            return new SonarQube(address).getIssues(auth, SonarQube.toResource(project), severity.toString());
        }else{
            assert rule != null;
            return new SonarQube(address).getIssuesByRule(auth, SonarQube.toResource(project), rule.getKey());
        }
    }

    @Override
    protected void done() {
        try {
            SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarTopComponent");
            sonarTopComponent.setIssues(severity == null ? rule: severity, get().toArray(new Issue[0]));
            sonarTopComponent.open();
            sonarTopComponent.requestVisible();
            sonarTopComponent.setProject(project);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof HttpException) {
                if (((HttpException) ex.getCause()).status() == 401) {
                    handle.finish();
                    Authentication authentication = AuthDialog.showAuthDialog(WindowManager.getDefault().getMainWindow());
                    if (authentication != null) {
                        IssuesWorker worker = new IssuesWorker(authentication, project, severity);
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
