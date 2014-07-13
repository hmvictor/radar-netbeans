package qubexplorer.ui;

import java.util.List;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.windows.WindowManager;
import qubexplorer.IssuesContainer;
import qubexplorer.RadarIssue;
import qubexplorer.SonarQube;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Victor
 */
public class IssuesWorker extends SonarQubeWorker<List<RadarIssue>, Void> {
    private Project project;
    private ProgressHandle handle;
    private IssueFilter[] filters;
    private IssuesContainer issuesContainer;
    
    public IssuesWorker(Project project, String url, String resourceKey, IssueFilter... filters) {
        super(url, resourceKey);
        this.project=project;
        this.filters=filters;
        issuesContainer=new SonarQube(getServerUrl());
        init();
    }
    
    public IssuesWorker(IssuesContainer container, Project project, String url, String resourceKey, IssueFilter... filters) {
        super(url, resourceKey);
        this.project=project;
        this.filters=filters;
        this.issuesContainer=container;
        init();
    }

    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.start();
        handle.switchToIndeterminate();
    }

    @Override
    protected List<RadarIssue> doInBackground() throws Exception {
        return issuesContainer.getIssues(getAuthentication(), getResourceKey(), filters);
    }

    @Override
    protected void success(List<RadarIssue> result) {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarTopComponent");
        sonarTopComponent.setIssues(filters, result.toArray(new RadarIssue[0]));
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        sonarTopComponent.setProject(project);
        sonarTopComponent.showIssuesList();
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
