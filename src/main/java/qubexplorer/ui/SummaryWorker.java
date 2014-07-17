package qubexplorer.ui;

import java.io.IOException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;
import qubexplorer.IssuesContainer;
import qubexplorer.filter.IssueFilter;
import qubexplorer.NoSuchProjectException;
import qubexplorer.SonarQube;
import qubexplorer.Summary;
import qubexplorer.ui.options.SonarQubeOptionsPanel;

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

    public SummaryWorker(IssuesContainer issuesContainer, Project project, String url, String resource, IssueFilter... filters) {
        super(url, resource);
        this.project = project;
        this.filters=filters;
        this.issuesContainer=issuesContainer;
        init();
    }

    public void setTriggerActionPlans(boolean triggerActionPlans) {
        this.triggerActionPlans = triggerActionPlans;
    }
    
    @Override
    protected Summary doInBackground() throws Exception {
        return issuesContainer.getSummary(getAuthentication(), getResourceKey(), filters);
    }

    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.start();
        handle.switchToIndeterminate();
    }

    @Override
    protected void success(Summary counting) {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        sonarTopComponent.setProject(project);
        sonarTopComponent.setSummary(counting);
        sonarTopComponent.setIssuesContainer(new SonarQube(getServerUrl()));
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        sonarTopComponent.showSummary();
        try {
            if(triggerActionPlans) {
                ActionPlansWorker workerPlans = new ActionPlansWorker(NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000"), SonarQube.toResource(project));
                workerPlans.execute();
            }
        } catch (IOException | XmlPullParserException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    protected void finished() {
        handle.finish();
    }
    
    @Override
    protected SonarQubeWorker createCopy() {
        SummaryWorker copy = new SummaryWorker(issuesContainer, project, getServerUrl(), getResourceKey());
        copy.setTriggerActionPlans(triggerActionPlans);
        return copy;
    }

    @Override
    protected void error(Throwable cause) {
        if(cause instanceof NoSuchProjectException) {
            if(getAuthentication() != null) {
                AuthenticationRepository.getInstance().saveAuthentication(getServerUrl(), null, getAuthentication());
            }
            ProjectChooser chooser=new ProjectChooser(WindowManager.getDefault().getMainWindow(), true);
            chooser.setSelectedUrl(getServerUrl());
            chooser.setServerUrlEnabled(false);
            chooser.loadProjectKeys();
            if(chooser.showDialog() == ProjectChooser.Option.ACCEPT) {
                scheduleWorker(new SummaryWorker(issuesContainer, project, getServerUrl(), chooser.getSelectedProjectKey()));
            }
        }else{
            super.error(cause);
        }
    }
    
}
