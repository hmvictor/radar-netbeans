package qubexplorer.filter;

import java.util.Objects;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;

/**
 *
 * @author Victor
 */
public class ActionPlanFilter implements IssueFilter{
    private final ActionPlan actionPlan;

    public ActionPlanFilter(ActionPlan actionPlan) {
        Objects.requireNonNull(actionPlan, "actionPlan is null");
        this.actionPlan = actionPlan;
    }
    
    @Override
    public void apply(IssueQuery query) {
        query.actionPlans(actionPlan.key());
    }

    @Override
    public boolean isValid(Issue issue) {
        return issue.actionPlan().equals(actionPlan.key());
    }
    
    @Override
    public String getDescription() {
        return "Action Plan: "+actionPlan.name();
    }
    
}
