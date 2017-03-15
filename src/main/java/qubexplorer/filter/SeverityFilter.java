package qubexplorer.filter;

import java.util.Objects;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueQuery;
import qubexplorer.Severity;

/**
 *
 * @author Victor
 */
public class SeverityFilter implements IssueFilter {

    private final Severity severity;

    public SeverityFilter(Severity severity) {
        Objects.requireNonNull(severity, "severity is null");
        this.severity = severity;
    }

    @Override
    public void apply(IssueQuery query) {
        query.severities(severity.toString().toUpperCase());
    }

    @Override
    public boolean isValid(Issue issue) {
        return issue.severity().equalsIgnoreCase(severity.toString());
    }

    @Override
    public String getDescription() {
        return "Severity: " + severity.toString();
    }

}
