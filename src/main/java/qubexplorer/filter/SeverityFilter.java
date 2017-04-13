package qubexplorer.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import qubexplorer.RadarIssue;
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
    public void apply(Map<String, List<String>> params) {
        params.put("severities", Arrays.asList(severity.toString().toUpperCase()));
    }
    
    @Override
    public boolean isValid(RadarIssue issue) {
        return issue.severity().equalsIgnoreCase(severity.toString());
    }

    @Override
    public String getDescription() {
        return "Severity: " + severity.toString();
    }

}
