package qubexplorer;

import java.util.List;
import org.sonar.wsclient.issue.Issue;

/**
 *
 * @author Víctor
 */
public interface ClassifierType<T extends Classifier> {
    
    T valueOf(Issue issue);
    
    List<T> getValues();
    
}
