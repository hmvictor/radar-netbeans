package qubexplorer.filter;

import java.util.Objects;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import qubexplorer.Rule;

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
    public boolean isValid(Issue issue) {
        return issue.ruleKey().equals(rule.getKey());
    }
    
    @Override
    public String getDescription() {
        return "Rule: "+rule.getName();
    }
    
}
