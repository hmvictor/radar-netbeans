package qubexplorer.filter;

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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String getDescription() {
        return "Type: "+type;
    }

    @Override
    public boolean isValid(RadarIssue issue) {
//        return issue.type() == type;
        throw new UnsupportedOperationException();
    }

//    @Override
    public Map<String, String> getParameters() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
