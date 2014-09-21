package qubexplorer.ui.task;

import java.util.List;
import org.openide.windows.WindowManager;
import qubexplorer.IssuesContainer;
import qubexplorer.RadarIssue;
import qubexplorer.filter.IssueFilter;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.ProjectContext;
import qubexplorer.ui.SonarIssuesTopComponent;

/**
 *
 * @author Victor
 */
public class IssuesTask extends Task<List<RadarIssue>>{
    private final IssuesContainer issuesContainer;
    private final IssueFilter[] filters;

    public IssuesTask(ProjectContext projectContext, IssuesContainer issuesContainer, IssueFilter[] filters) {
        super(projectContext, issuesContainer instanceof SonarQube? ((SonarQube)issuesContainer).getServerUrl(): null);
        this.issuesContainer=issuesContainer;
        this.filters=filters;
    }

    @Override
    public List<RadarIssue> execute() {
        return issuesContainer.getIssues(getToken(), getProjectContext().getProjectKey(), filters);
    }

    @Override
    protected void success(List<RadarIssue> result) {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        sonarTopComponent.setProjectContext(getProjectContext());
        sonarTopComponent.showIssues(filters, result.toArray(new RadarIssue[0]));
    }
    
}
