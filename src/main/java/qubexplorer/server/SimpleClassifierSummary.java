package qubexplorer.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import qubexplorer.Classifier;
import qubexplorer.Rule;
import qubexplorer.ClassifierSummary;

/**
 *
 * @author Victor
 */
public class SimpleClassifierSummary<T extends Classifier> implements ClassifierSummary<T> {

    private final Map<Classifier, Map<Rule, Integer>> countsByClassifier=new HashMap<>();

    public SimpleClassifierSummary() {
        
    }
    
    public void increment(T clasifier, Rule rule, int increment) {
        Map<Rule, Integer> countByRule = countsByClassifier.get(clasifier);
        if(countByRule == null ){
            countByRule=new HashMap<>();
            countsByClassifier.put(clasifier, countByRule);
        }
        Integer count = countByRule.get(rule);
        if (count == null) {
            count = increment;
        } else {
            count = count + increment;
        }
        countByRule.put(rule, count);
    }

    @Override
    public int getCount(T classifier) {
        Map<Rule, Integer> map = countsByClassifier.get(classifier);
        if (map == null) {
            return 0;
        } else {
            int sum=0;
            for (Map.Entry<Rule, Integer> entry : map.entrySet()) {
                sum+=entry.getValue();
            }
            return sum;
        }
    }

    @Override
    public int getCount(Rule rule) {
        int count = 0;
        for (Map<Rule, Integer> countByRule : countsByClassifier.values()) {
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
        for (Map<Rule, Integer> countByRule : countsByClassifier.values()) {
            for (Integer ruleCount : countByRule.values()) {
                suma += ruleCount;
            }
        }
        return suma;
    }

    @Override
    public Set<Rule> getRules(T classifier) {
        if (countsByClassifier.containsKey(classifier)) {
            return countsByClassifier.get(classifier).keySet();
        } else {
            return Collections.emptySet();
        }
    }

//    @Override
//    public List<T> getClassifierValues() {
//        throw new UnsupportedOperationException("Not supported yet.");
//    }

}
