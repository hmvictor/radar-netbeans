package qubexplorer;

import java.util.Set;

/**
 *
 * @author Victor
 */
public interface Summary {

    int getCount(Severity severity);

    int getCount(Rule rule);

    int getCount();

    Set<Rule> getRules(Severity severity);
    
}
