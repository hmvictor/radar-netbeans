package qubexplorer.server.ui;

import java.util.Collections;
import java.util.List;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.issue.ActionPlan;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.ProjectContext;
import qubexplorer.ui.SonarIssuesTopComponent;
import qubexplorer.ui.task.Task;

/**
 *
 * @author Victor
 */
public class ActionPlansTask extends Task<List<ActionPlan>>{
    private final SonarQube sonarQube;

    public ActionPlansTask(SonarQube sonarQube, ProjectContext projectContext) {
        super(projectContext, sonarQube.getServerUrl());
        this.sonarQube=sonarQube;
    }

    @Override
    public List<ActionPlan> execute() {
        try{
            return sonarQube.getActionPlans(getUserCredentials(), getProjectContext().getConfiguration().getKey());
        }catch(HttpException ex){
            if(ex.status() == 404){
                return Collections.emptyList();
            }else{
                throw ex;
            }
        }
    }

    @Override
    protected void success(List<ActionPlan> result) {
        SonarIssuesTopComponent issuesTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        issuesTopComponent.setActionPlansOptions(result);
        issuesTopComponent.open();
        issuesTopComponent.requestVisible();
    }
    
}
