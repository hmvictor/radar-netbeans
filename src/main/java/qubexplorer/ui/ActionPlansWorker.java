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
    private ProgressHandle handle;

    public ActionPlansWorker(String serverUrl, String resourceKey) {
        super(serverUrl, resourceKey);
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.start();
        handle.switchToIndeterminate();
    }

    @Override
    protected SonarQubeWorker createCopy() {
        return new ActionPlansWorker(getServerUrl(), getResourceKey());
    }

    @Override
    protected List<ActionPlan> doInBackground() throws Exception {
        return new SonarQube(getServerUrl()).getActionPlans(getAuthentication(), getResourceKey());
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
