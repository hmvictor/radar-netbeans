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
public class AsigneesFilter implements IssueFilter {

    private final Set<String> asignees;

    public AsigneesFilter(Set<String> asignees) {
        this.asignees = asignees;
    }
    
    public AsigneesFilter(String... asignees) {
        this.asignees=new HashSet<>(Arrays.asList(asignees));
    }

    @Override
    public void apply(Map<String, List<String>> params) {
        params.put("asignees", new ArrayList<>(asignees));
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
