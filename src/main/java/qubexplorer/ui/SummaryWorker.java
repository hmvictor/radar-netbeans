package qubexplorer.ui;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.windows.WindowManager;
import qubexplorer.IssuesContainer;
import qubexplorer.filter.IssueFilter;
import qubexplorer.NoSuchProjectException;
import qubexplorer.server.SonarQube;
import qubexplorer.Summary;

/**
 *
 * @author Victor
 */
class SummaryWorker extends SonarQubeWorker<Summary, Void> {
    private ProgressHandle handle;
    private Project project;
    private IssueFilter[] filters;
    private boolean triggerActionPlans;
    private IssuesContainer issuesContainer;

    public SummaryWorker(IssuesContainer issuesContainer, Project project, String resource, IssueFilter... filters) {
        super(resource);
        this.project = project;
        this.filters=filters;
        this.issuesContainer=issuesContainer;
        if(issuesContainer instanceof SonarQube) {
            setServerUrl(((SonarQube)issuesContainer).getServerUrl());
        }
        init();
    }

    public void setTriggerActionPlans(boolean triggerActionPlans) {
        this.triggerActionPlans = triggerActionPlans;
    }
    
    @Override
    protected Summary doInBackground() throws Exception {
        return issuesContainer.getSummary(getAuthentication(), getProjectKey(), filters);
    }

    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.start();
        handle.switchToIndeterminate();
    }

    @Override
    protected void success(Summary summary) {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        sonarTopComponent.setProjectContext(new ProjectContext(project, getProjectKey()));
        sonarTopComponent.setIssuesContainer(issuesContainer);
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        sonarTopComponent.showSummary(summary);
        if(triggerActionPlans) {
            scheduleWorker(new ActionPlansWorker(SonarQubeFactory.createForDefaultServerUrl(), getProjectKey()));
        }
    }

    @Override
    protected void finished() {
        handle.finish();
    }
    
    @Override
    protected SonarQubeWorker createCopy() {
        SummaryWorker copy = new SummaryWorker(issuesContainer, project, getProjectKey());
        copy.setTriggerActionPlans(triggerActionPlans);
        return copy;
    }

    @Override
    protected void error(Throwable cause) {
        if(cause instanceof NoSuchProjectException) {
            //TODO
            assert issuesContainer instanceof SonarQube;
            SonarQube sonarQube=(SonarQube) issuesContainer;
            if(getAuthentication() != null) {
                AuthenticationRepository.getInstance().saveAuthentication(sonarQube.getServerUrl(), null, getAuthentication());
            }
            ProjectChooser chooser=new ProjectChooser(WindowManager.getDefault().getMainWindow(), true);
            chooser.setSelectedUrl(sonarQube.getServerUrl());
            chooser.setServerUrlEnabled(false);
            chooser.loadProjectKeys();
            if(chooser.showDialog() == ProjectChooser.Option.ACCEPT) {
                scheduleWorker(new SummaryWorker(issuesContainer, project, chooser.getSelectedProjectKey()));
            }
        }else{
            super.error(cause);
        }
    }
    
}
