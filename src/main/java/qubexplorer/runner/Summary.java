package qubexplorer.runner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sonar.wsclient.services.Rule;
import qubexplorer.Severity;
import qubexplorer.runner.SonarRunnerResult.IntWrapper;

/**
 *
 * @author Victor
 */
public class Summary {
    private Map<String, IntWrapper> countsBySeverity;
    private Map<String, IntWrapper> countsByRule;
    private Map<Severity, Set<Rule>> rules;

    Summary(Map<String, IntWrapper> countsBySeverity, Map<String, IntWrapper> countsByRule, Map<Severity, Set<Rule>> rules) {
        this.countsBySeverity = countsBySeverity;
        this.countsByRule = countsByRule;
        this.rules=rules;
    }

    public Summary() {
        this(new HashMap<String, IntWrapper>(), new HashMap<String, IntWrapper>(), new HashMap<Severity, Set<Rule>>());
    }
    
    public int getCount(Severity severity) {
        IntWrapper count = countsBySeverity.get(severity.toString());
        return count != null? count.getInt(): 0;
    }
    
    public int getCount(Rule rule) {
        IntWrapper count = countsByRule.get(rule.getKey());
        return count != null? count.getInt(): 0;
    }
    
    public int getCount(){
        int suma=0;
        for (Map.Entry<String, IntWrapper> entry : countsBySeverity.entrySet()) {
            suma+=entry.getValue().getInt();
        }
        return suma;
    }
    
    public Set<Rule> getRules(Severity severity) {
        if(rules.containsKey(severity)) {
            return rules.get(severity);
        }else{
            return Collections.emptySet();
        }
    }
    
}
