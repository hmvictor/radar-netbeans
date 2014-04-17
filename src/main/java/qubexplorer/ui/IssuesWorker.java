package qubexplorer.ui;

import java.util.List;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.services.Rule;
import qubexplorer.IssueDecorator;
import qubexplorer.filter.IssueFilter;
import qubexplorer.Severity;
import qubexplorer.SonarQube;

/**
 *
 * @author Victor
 */
public class IssuesWorker extends SonarQubeWorker<List<IssueDecorator>, Void> {
    private Project project;
    private ProgressHandle handle;
    private IssueFilter[] filters;
    
    public IssuesWorker(Project project, String url, String resourceKey, IssueFilter... filters) {
        super(url, resourceKey);
        this.project=project;
        this.filters=filters;
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
        return sonarQube.getIssues(getAuthentication(), getResourceKey(), filters);
    }

    @Override
    protected void success(List<IssueDecorator> result) {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarTopComponent");
        sonarTopComponent.setIssues(filters, result.toArray(new IssueDecorator[0]));
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        sonarTopComponent.setProject(project);
    }

    @Override
    protected SonarQubeWorker createCopy() {
        return new IssuesWorker(project, getServerUrl(), getResourceKey(), filters);
    }

    @Override
    protected void finished() {
        handle.finish();
    }
    
}
