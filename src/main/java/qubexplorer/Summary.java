package qubexplorer;

import java.util.Set;
import org.sonar.wsclient.services.Rule;

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
