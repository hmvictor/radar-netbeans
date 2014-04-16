package qubexplorer;

import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.IssueQuery;

/**
 *
 * @author Victor
 */
public class ActionPlanFilter implements IssueFilter{
    private ActionPlan actionPlan;

    public ActionPlanFilter(ActionPlan actionPlan) {
        this.actionPlan = actionPlan;
    }
    
    @Override
    public void apply(IssueQuery query) {
        if(actionPlan != null){
            query.actionPlans(actionPlan.key());
        }
    }
    
}
