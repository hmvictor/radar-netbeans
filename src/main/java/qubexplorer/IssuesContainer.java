package qubexplorer;

import java.util.List;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Victor
 */
public interface IssuesContainer {
    
    List<RadarIssue> getIssues(AuthenticationToken auth, String projectKey, IssueFilter... filters);

    Summary getSummary(AuthenticationToken authentication, String projectKey, IssueFilter[] filters);
    
}
