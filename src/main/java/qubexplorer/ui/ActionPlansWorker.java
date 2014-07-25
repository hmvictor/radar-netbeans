package qubexplorer.ui;

import java.util.List;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.issue.ActionPlan;
import qubexplorer.server.SonarQube;

/**
 *
 * @author Victor
 */
public class ActionPlansWorker extends SonarQubeWorker<List<ActionPlan>, Void>{
    private final ProgressHandle handle;
    private final SonarQube sonarQube;

    public ActionPlansWorker(SonarQube sonarQube, String resourceKey) {
        super(resourceKey);
        this.sonarQube=sonarQube;
        setServerUrl(sonarQube.getServerUrl());
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.start();
        handle.switchToIndeterminate();
    }

    @Override
    protected SonarQubeWorker createCopy() {
        return new ActionPlansWorker(sonarQube, getProjectKey());
    }

    @Override
    protected List<ActionPlan> doInBackground() throws Exception {
        return sonarQube.getActionPlans(getAuthentication(), getProjectKey());
    }

    @Override
    protected void success(List<ActionPlan> result) {
        SonarIssuesTopComponent issuesTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        issuesTopComponent.setActionPlans(result);
        issuesTopComponent.open();
        issuesTopComponent.requestVisible();
    }

    @Override
    protected void finished() {
        handle.finish();
    }
    
}
