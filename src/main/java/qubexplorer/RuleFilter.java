package qubexplorer;

import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.Rule;

/**
 *
 * @author Victor
 */
public class RuleFilter implements IssueFilter{
    private Rule rule;

    public RuleFilter(Rule rule) {
        this.rule=rule;
    }
    
    @Override
    public void apply(IssueQuery query) {
        if(rule != null) {
            query.rules(rule.getKey());
        }
    }
    
}
