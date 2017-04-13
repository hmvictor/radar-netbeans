package qubexplorer.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    public void apply(Map<String, List<String>> params) {
        params.put("rules", Arrays.asList(rule.getKey()));
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
