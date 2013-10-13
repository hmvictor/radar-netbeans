package qubexplorer;

import java.util.HashMap;
import java.util.Map;
import org.sonar.wsclient.services.Rule;

/**
 *
 * @author Victor
 */
public class Counting {
    private Map<Severity, Map<Rule, Integer>> severityCounts=new HashMap<>();
    
    public int getCount(Severity severity) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    public Map<Rule, Integer> getRuleCounts(Severity severity) {
        if(severityCounts.containsKey(severity)) {
            return severityCounts.get(severity);
        }else{
            return new HashMap<>();
        }
    }

    public void setRuleCounts(Severity severity, Map<Rule, Integer> counts) {
        severityCounts.put(severity, counts);
    }
    
}
