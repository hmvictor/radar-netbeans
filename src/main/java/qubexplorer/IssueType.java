package qubexplorer;

import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;
import qubexplorer.filter.IssueFilter;
import qubexplorer.filter.TypeFilter;

/**
 *
 * @author Victor
 */
public enum IssueType implements Classifier {
    
    BUG("Bug"),
    VULNERABILITY("Vulnerability"),
    CODE_SMELL("Code smell");
    
    private final String userDescription;

    private IssueType(String userDescription) {
        this.userDescription = userDescription;
    }
    
    @Override
    public IssueFilter createFilter() {
        return new TypeFilter(this);
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getUserDescription() {
        return userDescription;
    }

    public static ClassifierType<IssueType> getType() {
        return INSTANCE;
    }
    
    private static final ClassifierType<IssueType> INSTANCE=new IssueTypeClassifier();
    
    private static class IssueTypeClassifier implements ClassifierType<IssueType>{

        @Override
        public IssueType valueOf(RadarIssue issue) {
            return IssueType.valueOf(issue.type().toUpperCase());
        }

        @Override
        public List<IssueType> getValues() {
            return Arrays.asList(IssueType.values());
        }
        
    }
    
}
