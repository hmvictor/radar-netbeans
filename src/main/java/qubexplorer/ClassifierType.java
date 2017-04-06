package qubexplorer;

import java.util.List;

/**
 *
 * @author Víctor
 */
public interface ClassifierType<T extends Classifier> {
    
    T valueOf(RadarIssue issue);
    
    List<T> getValues();
    
}
