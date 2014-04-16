package qubexplorer;

import org.sonar.wsclient.issue.IssueQuery;

/**
 *
 * @author Victor
 */
public class SeverityFilter implements IssueFilter{
    private Severity severity;

    public SeverityFilter(Severity severity) {
        this.severity = severity;
    }
    
    @Override
    public void apply(IssueQuery query) {
        if(severity != null) {
            query.severities(severity.toString().toUpperCase());
        }
    }
    
}
