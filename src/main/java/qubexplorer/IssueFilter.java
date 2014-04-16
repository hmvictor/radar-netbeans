package qubexplorer;

import org.sonar.wsclient.issue.IssueQuery;

/**
 *
 * @author Victor
 */
public interface IssueFilter {
    
    void apply(IssueQuery query);
    
}
