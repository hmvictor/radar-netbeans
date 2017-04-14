package qubexplorer.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import qubexplorer.RadarIssue;

/**
 *
 * @author Víctor
 */
public class AssigneesFilter implements IssueFilter {

    private final Set<String> assignees;

    public AssigneesFilter(Set<String> asignees) {
        this.assignees = asignees;
    }
    
    public AssigneesFilter(String... asignees) {
        this.assignees=new HashSet<>(Arrays.asList(asignees));
    }

    public Set<String> getAssignees() {
        return assignees;
    }
    
    @Override
    public void apply(Map<String, List<String>> params) {
        params.put("assignees", new ArrayList<>(assignees));
    }
    
    @Override
    public String getDescription() {
        return "Assignees: " + String.join(", ", assignees);
    }

    @Override
    public boolean isValid(RadarIssue issue) {
        return assignees.contains(issue.assignee());
    }

}
