package qubexplorer.ui;

import org.openide.windows.WindowManager;
import qubexplorer.IssuesContainer;
import qubexplorer.NoSuchProjectException;
import qubexplorer.Summary;
import qubexplorer.filter.IssueFilter;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.task.Task;
import qubexplorer.ui.task.TaskExecutor;

/**
 *
 * @author Victor
 */
public class SummaryTask extends Task<Summary>{
    private final IssuesContainer issuesContainer;
    private final IssueFilter[] filters;

    public SummaryTask(IssuesContainer issuesContainer, ProjectContext projectContext, IssueFilter[] filters) {
        super(projectContext, issuesContainer instanceof SonarQube? ((SonarQube)issuesContainer).getServerUrl(): null);
        this.issuesContainer = issuesContainer;
        this.filters = filters;
    }

    @Override
    protected void init() {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        sonarTopComponent.resetState();
    }
    
    @Override
    public Summary execute() {
        return issuesContainer.getSummary(getUserCredentials(), getProjectContext().getConfiguration().getKey(), filters);
    }
    
    @Override
    protected void success(Summary summary) {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        sonarTopComponent.setProjectContext(getProjectContext());
        sonarTopComponent.setIssuesContainer(issuesContainer);
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        sonarTopComponent.showSummary(summary);
    }

    @Override
    protected void fail(Throwable cause) {
        if(cause instanceof NoSuchProjectException) {
            assert issuesContainer instanceof SonarQube;
            SonarQube sonarQube=(SonarQube) issuesContainer;
            if(getUserCredentials()!= null) {
                AuthenticationRepository.getInstance().saveAuthentication(sonarQube.getServerUrl(), null, getUserCredentials());
            }
            ProjectChooser chooser=new ProjectChooser(WindowManager.getDefault().getMainWindow(), true);
            chooser.setSelectedUrl(sonarQube.getServerUrl());
            chooser.setServerUrlEnabled(false);
            chooser.loadProjectKeys();
            if(chooser.showDialog() == ProjectChooser.Option.ACCEPT) {
                ProjectContext newProjectContext = new ProjectContext(getProjectContext().getProject(), chooser.getSelectedProject());
                TaskExecutor.execute(new SummaryTask(issuesContainer, newProjectContext, filters));
            }
        }else{
            super.fail(cause);
        }
    }
    
}
