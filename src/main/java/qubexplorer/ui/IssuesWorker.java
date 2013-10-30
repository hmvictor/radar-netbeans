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
public class IssuesWorker extends SonarQubeWorker<List<IssueDecorator>, Void> {
    private Project project;
    private Severity severity;
    private Rule rule;
    private ProgressHandle handle;

    public IssuesWorker(Project project, Severity severity, String url, String resourceKey) {
        super(url, resourceKey);
        this.project=project;
        this.severity=severity;
        init();
    }
    
    public IssuesWorker(Project project, Rule rule, String url, String resourceKey) {
        super(url, resourceKey);
        this.project=project;
        this.rule=rule;
        init();
    }
    
    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.start();
        handle.switchToIndeterminate();
    }

    @Override
    protected List<IssueDecorator> doInBackground() throws Exception {
        SonarQube sonarQube = new SonarQube(getServerUrl());
        if(severity != null) {
            return sonarQube.getIssuesBySeverity(getAuthentication(), getResourceKey(), severity.toString());
        }else if(rule != null){
            return sonarQube.getIssuesByRule(getAuthentication(), getResourceKey(), rule.getKey());
        }else{
            return sonarQube.getIssuesBySeverity(getAuthentication(), getResourceKey(), "any");
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
        IssuesWorker worker;
        if(rule != null){
            worker= new IssuesWorker(project, rule, getServerUrl(), getResourceKey());
        }else{
            worker= new IssuesWorker(project, severity, getServerUrl(), getResourceKey());
        }
        return worker;
    }

    @Override
    protected void finished() {
        handle.finish();
    }
    
}
