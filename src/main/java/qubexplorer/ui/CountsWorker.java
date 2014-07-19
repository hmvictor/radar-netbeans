package qubexplorer.ui;

import java.io.IOException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;
import qubexplorer.server.Counting;
import qubexplorer.filter.IssueFilter;
import qubexplorer.NoSuchProjectException;
import qubexplorer.server.SonarQube;

/**
 *
 * @author Victor
 */
@Deprecated
class CountsWorker extends SonarQubeWorker<Counting, Void> {
    private ProgressHandle handle;
    private Project project;
    private IssueFilter[] filters;
    private boolean triggerActionPlans;
    private SonarQube sonarQube;

    public CountsWorker(SonarQube sonarQube, Project project, String resource, IssueFilter... filters) {
        super(resource);
        this.project = project;
        this.filters=filters;
        setServerUrl(sonarQube.getServerUrl());
        init();
    }

    public void setTriggerActionPlans(boolean triggerActionPlans) {
        this.triggerActionPlans = triggerActionPlans;
    }
    
    @Override
    protected Counting doInBackground() throws Exception {
        return sonarQube.getCounting(getAuthentication(), getResourceKey(), filters);
    }

    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.start();
        handle.switchToIndeterminate();
    }

    @Override
    protected void success(Counting counting) {
        SonarMainTopComponent infoTopComponent = (SonarMainTopComponent) WindowManager.getDefault().findTopComponent("InfoTopComponent");
        infoTopComponent.setProject(project);
        infoTopComponent.setCounting(counting);
        infoTopComponent.setSonarQubeUrl(sonarQube.getServerUrl());
        infoTopComponent.setResourceKey(getResourceKey());
        infoTopComponent.open();
        infoTopComponent.requestVisible();
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        sonarTopComponent.setProject(project);
        sonarTopComponent.setSummary(counting);
        sonarTopComponent.setIssuesContainer(sonarQube);
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        sonarTopComponent.showSummary();
        try {
            if(triggerActionPlans) {
                ActionPlansWorker workerPlans = new ActionPlansWorker(SonarQubeFactory.createForDefaultServerUrl(), SonarQube.toResource(project));
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
        CountsWorker copy = new CountsWorker(sonarQube, project, getResourceKey());
        copy.setTriggerActionPlans(triggerActionPlans);
        return copy;
    }

    @Override
    protected void error(Throwable cause) {
        if(cause instanceof NoSuchProjectException) {
            if(getAuthentication() != null) {
                AuthenticationRepository.getInstance().saveAuthentication(sonarQube.getServerUrl(), null, getAuthentication());
            }
            ProjectChooser chooser=new ProjectChooser(WindowManager.getDefault().getMainWindow(), true);
            chooser.setSelectedUrl(sonarQube.getServerUrl());
            chooser.setServerUrlEnabled(false);
            chooser.loadProjectKeys();
            if(chooser.showDialog() == ProjectChooser.Option.ACCEPT) {
                scheduleWorker(new CountsWorker(sonarQube, project, chooser.getSelectedProjectKey()));
            }
        }else{
            super.error(cause);
        }
    }
    
}
