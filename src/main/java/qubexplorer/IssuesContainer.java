package qubexplorer;

import java.util.List;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Victor
 */
public interface IssuesContainer {
    
    List<RadarIssue> getIssues(UserCredentials auth, ResourceKey projectKey, IssueFilter... filters);

    Summary getSummary(UserCredentials authentication, ResourceKey projectKey, IssueFilter[] filters);
    
}
