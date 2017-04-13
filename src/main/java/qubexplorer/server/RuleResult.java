package qubexplorer.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import qubexplorer.Rule;

/**
 *
 * @author Víctor
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleResult {
    private Rule rule;

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }
    
}
