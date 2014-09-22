package qubexplorer;

import java.util.List;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Victor
 */
public interface IssuesContainer {
    
    List<RadarIssue> getIssues(UserCredentials auth, String projectKey, IssueFilter... filters);

    Summary getSummary(UserCredentials authentication, String projectKey, IssueFilter[] filters);
    
}
