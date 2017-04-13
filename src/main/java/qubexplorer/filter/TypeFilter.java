package qubexplorer.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import qubexplorer.IssueType;
import qubexplorer.RadarIssue;


public class TypeFilter implements IssueFilter {
    private final IssueType type;

    public TypeFilter(IssueType type) {
        this.type = type;
    }
    
    @Override
    public void apply(Map<String, List<String>> params) {
        params.put("types", Arrays.asList(type.toString().toUpperCase()));
    }
    
    @Override
    public String getDescription() {
        return "Type: "+type;
    }

    @Override
    public boolean isValid(RadarIssue issue) {
        return issue.type().equalsIgnoreCase(type.toString());
    }

}
