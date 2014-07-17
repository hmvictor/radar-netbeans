package qubexplorer;

import java.util.List;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Victor
 */
public interface IssuesContainer {
    
    List<RadarIssue> getIssues(Authentication auth, String resource, IssueFilter... filters);

    Summary getSummary(Authentication authentication, String resourceKey, IssueFilter[] filters);
    
}
