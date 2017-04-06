package qubexplorer.filter;

import java.util.Objects;
import org.sonar.wsclient.issue.IssueQuery;
import qubexplorer.RadarIssue;
import qubexplorer.Rule;

/**
 *
 * @author Victor
 */
public class RuleFilter implements IssueFilter {

    private final Rule rule;

    public RuleFilter(Rule rule) {
        Objects.requireNonNull(rule, "rule is null");
        this.rule = rule;
    }

    @Override
    public void apply(IssueQuery query) {
        query.rules(rule.getKey());
    }

    @Override
    public boolean isValid(RadarIssue issue) {
        return issue.ruleKey().equals(rule.getKey());
    }

    @Override
    public String getDescription() {
        return "Rule: " + rule.getName();
    }

}
