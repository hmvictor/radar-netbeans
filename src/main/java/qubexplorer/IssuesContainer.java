package qubexplorer;

import java.util.List;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Victor
 */
public interface IssuesContainer {
    
    List<RadarIssue> getIssues(AuthenticationToken auth, String resource, IssueFilter... filters);

    Summary getSummary(AuthenticationToken authentication, String resourceKey, IssueFilter[] filters);
    
}
