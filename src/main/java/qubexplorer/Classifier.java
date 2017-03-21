package qubexplorer;

import javax.swing.Icon;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Víctor
 */
public interface Classifier {

    IssueFilter createFilter();

    Icon getIcon();

    String getUserDescription();
    
}
