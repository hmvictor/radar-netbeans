package qubexplorer.filter;

import java.util.Map;
import org.sonar.wsclient.issue.IssueQuery;
import qubexplorer.IssueType;
import qubexplorer.RadarIssue;


public class TypeFilter implements IssueFilter {
    private final IssueType type;

    public TypeFilter(IssueType type) {
        this.type = type;
    }
    
    @Override
    public void apply(IssueQuery query) {
//        query.type(type);
        throw new UnsupportedOperationException();
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
