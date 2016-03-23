package qubexplorer.server;

import java.util.Collections;
import java.util.EnumMap;
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
public class SimpleSummary implements Summary {

    private final Map<Severity, Map<Rule, Integer>> countsBySeverity = new EnumMap<>(Severity.class);

    public SimpleSummary() {
        for (Severity severity : Severity.values()) {
            Map<Rule, Integer> counts = new HashMap<>();
            countsBySeverity.put(severity, counts);
        }
    }

    @Override
    public int getCount(Severity severity) {
        Map<Rule, Integer> map = countsBySeverity.get(severity);
        if (map == null) {
            return 0;
        } else {
            int sum = 0;
            for (Integer i : map.values()) {
                sum += i;
            }
            return sum;
        }
    }

    public Map<Rule, Integer> getRuleCounts(Severity severity) {
        if (countsBySeverity.containsKey(severity)) {
            return countsBySeverity.get(severity);
        } else {
            return new HashMap<>();
        }
    }

    @Override
    public int getCount(Rule rule) {
        int count = 0;
        for (Map<Rule, Integer> countByRule : countsBySeverity.values()) {
            if (countByRule.containsKey(rule)) {
                count = countByRule.get(rule);
                break;
            }
        }
        return count;
    }

    @Override
    public int getCount() {
        int suma = 0;
        for (Map<Rule, Integer> map : countsBySeverity.values()) {
            for (Integer integer : map.values()) {
                suma += integer;
            }
        }
        return suma;
    }

    @Override
    public Set<Rule> getRules(Severity severity) {
        if (countsBySeverity.containsKey(severity)) {
            return countsBySeverity.get(severity).keySet();
        } else {
            return Collections.emptySet();
        }
    }

    public void increment(Severity severity, Rule rule, int increment) {
        Map<Rule, Integer> countByRule = countsBySeverity.get(severity);
        Integer count = countByRule.get(rule);
        if (count == null) {
            count = 1;
        } else {
            count = count + 1;
        }
        countByRule.put(rule, count);
    }

}
