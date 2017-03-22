package qubexplorer;

import java.util.List;
import java.util.Set;

/**
 *
 * @author Victor
 */
public interface ClassifierSummary<T extends Classifier>{

    int getCount(T classifier);

    int getCount(Rule rule);

    int getCount();

    Set<Rule> getRules(T classifier);
    
//    List<T> getClassifierValues();
    
}
