package qubexplorer.filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.sonar.wsclient.issue.IssueQuery;
import qubexplorer.RadarIssue;

/**
 *
 * @author Víctor
 */
public class AsigneesFilter implements IssueFilter {

    private final Set<String> asignees;

    public AsigneesFilter(Set<String> asignees) {
        this.asignees = asignees;
    }
    
    public AsigneesFilter(String... asignees) {
        this.asignees=new HashSet<>(Arrays.asList(asignees));
    }

    @Override
    public void apply(IssueQuery query) {
        query.assignees(asignees.toArray(new String[0]));
    }

    @Override
    public String getDescription() {
        return "Asignees: " + asignees.toString();
    }

    @Override
    public boolean isValid(RadarIssue issue) {
        return asignees.contains(issue.assignee());
    }

}
