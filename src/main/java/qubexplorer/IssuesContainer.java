package qubexplorer;

import java.util.List;
import org.sonar.wsclient.issue.Issue;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Victor
 */
public interface IssuesContainer {
    
    List<Issue> getIssues(Authentication auth, String resource, IssueFilter... filters);
    
}
