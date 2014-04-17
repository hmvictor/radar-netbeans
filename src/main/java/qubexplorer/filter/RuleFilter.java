package qubexplorer.filter;

import java.util.Objects;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.services.Rule;

/**
 *
 * @author Victor
 */
public class RuleFilter implements IssueFilter{
    private Rule rule;

    public RuleFilter(Rule rule) {
        Objects.requireNonNull(rule, "rule is null");
        this.rule=rule;
    }
    
    @Override
    public void apply(IssueQuery query) {
        query.rules(rule.getKey());
    }

    @Override
    public String getDescription() {
        return "Rule: "+rule.getTitle();
    }
    
}
