package qubexplorer.filter;

import org.sonar.wsclient.issue.IssueQuery;
import qubexplorer.RadarIssue;

/**
 *
 * @author Victor
 */
public interface IssueFilter {

    void apply(IssueQuery query);

    String getDescription();

    boolean isValid(RadarIssue issue);

}
