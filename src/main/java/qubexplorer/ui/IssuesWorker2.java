package qubexplorer.ui;

import java.util.List;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.services.Rule;
import qubexplorer.Authentication;
import qubexplorer.IssueDecorator;
import qubexplorer.Severity;
import qubexplorer.SonarQube;

/**
 *
 * @author Victor
 */
public class IssuesWorker2 extends SonarQubeWorker<List<IssueDecorator>, Void> {
    private Project project;
    private Severity severity;
    private Rule rule;
    private Authentication auth;
    private ProgressHandle handle;

    public IssuesWorker2(Project project, Severity severity, String url, String resourceKey) {
        super(url, resourceKey);
        this.project=project;
        this.severity=severity;
        init();
    }
    
    public IssuesWorker2(Project project, Rule rule, String url, String resourceKey) {
        super(url, resourceKey);
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
        SonarQube sonarQube = new SonarQube(getServerUrl());
        if(severity != null) {
            return sonarQube.getIssuesBySeverity(auth, getResourceKey(), severity.toString());
        }else if(rule != null){
            return sonarQube.getIssuesByRule(auth, getResourceKey(), rule.getKey());
        }else{
            return sonarQube.getIssuesBySeverity(auth, getResourceKey(), "any");
        }
    }

    @Override
    protected void success(List<IssueDecorator> result) {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarTopComponent");
        sonarTopComponent.setIssues(severity == null ? rule: severity, result.toArray(new IssueDecorator[0]));
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        sonarTopComponent.setProject(project);
    }

    @Override
    protected SonarQubeWorker createCopy() {
        IssuesWorker2 worker;
        if(rule != null){
            worker= new IssuesWorker2(project, rule, getServerUrl(), getResourceKey());
        }else{
            worker= new IssuesWorker2(project, severity, getServerUrl(), getResourceKey());
        }
        return worker;
    }

    @Override
    protected void finished() {
        handle.finish();
    }
    
}
