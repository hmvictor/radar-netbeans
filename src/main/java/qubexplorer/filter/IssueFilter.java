package qubexplorer.filter;

import org.sonar.wsclient.issue.IssueQuery;

/**
 *
 * @author Victor
 */
public interface IssueFilter {
    
    void apply(IssueQuery query);

    String getDescription();
    
}
