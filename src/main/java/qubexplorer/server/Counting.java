package qubexplorer.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sonar.wsclient.services.Rule;
import qubexplorer.Severity;
import qubexplorer.Summary;

/**
 *
 * @author Victor
 */
public class Counting implements Summary{
    private Map<Severity, Map<Rule, Integer>> severityCounts=new HashMap<>();
    private double rulesCompliance;
    
    public int getCount(Severity severity) {
        Map<Rule, Integer> map = severityCounts.get(severity);
        if(map == null) {
            return 0;
        }else{
            int sum=0;
            for(Integer i:map.values()) {
                sum+=i;
            }
            return sum;
        }
    }

    public double getRulesCompliance() {
        return rulesCompliance;
    }

    public void setRulesCompliance(double rulesCompliance) {
        this.rulesCompliance = rulesCompliance;
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

    @Override
    public int getCount(Rule rule) {
        for (Map<Rule, Integer> map : severityCounts.values()) {
            for (Map.Entry<Rule, Integer> entry : map.entrySet()) {
                if(entry.getKey().getKey().equals(rule.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return 0;
    }

    @Override
    public int getCount() {
        int suma=0;
        for (Map<Rule, Integer> map : severityCounts.values()) {
            for (Integer integer : map.values()) {
                suma+=integer;
            }
        }
        return suma;
    }

    @Override
    public Set<Rule> getRules(Severity severity) {
        if(severityCounts.containsKey(severity)) {
            return severityCounts.get(severity).keySet();
        }else{
            return Collections.emptySet();
        }
    }
    
}
