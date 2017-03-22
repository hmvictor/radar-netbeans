package qubexplorer.runner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import qubexplorer.Classifier;
import qubexplorer.ClassifierSummary;
import qubexplorer.Rule;
import qubexplorer.runner.SonarRunnerResult.IntWrapper;

/**
 *
 * @author Victor
 */
public class SonarRunnerClassifierSummary<T extends Classifier> implements ClassifierSummary<T> {
    private Map<String, IntWrapper> countsByClassifier;
    private Map<String, IntWrapper> countsByRule;
    private Map<T, Set<Rule>> rules;

    SonarRunnerClassifierSummary(Map<String, IntWrapper> countsBySeverity, Map<String, IntWrapper> countsByRule, Map<T, Set<Rule>> rules) {
        this.countsByClassifier = countsBySeverity;
        this.countsByRule = countsByRule;
        this.rules=rules;
    }

    public SonarRunnerClassifierSummary() {
        this(new HashMap<String, IntWrapper>(), new HashMap<String, IntWrapper>(), new HashMap<T, Set<Rule>>());
    }
    
    @Override
    public int getCount(T classifier) {
        IntWrapper count = countsByClassifier.get(classifier.toString());
        return count != null? count.getInt(): 0;
    }
    
    @Override
    public int getCount(Rule rule) {
        IntWrapper count = countsByRule.get(rule.getKey());
        return count != null? count.getInt(): 0;
    }
    
    @Override
    public int getCount(){
        int suma=0;
        for (Map.Entry<String, IntWrapper> entry : countsByClassifier.entrySet()) {
            suma+=entry.getValue().getInt();
        }
        return suma;
    }
    
    @Override
    public Set<Rule> getRules(T classifier) {
        if(rules.containsKey(classifier)) {
            return rules.get(classifier);
        }else{
            return Collections.emptySet();
        }
    }

//    @Override
//    public List<T> getClassifierValues() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//    }
    
}
