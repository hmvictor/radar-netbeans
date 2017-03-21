package qubexplorer;

import javax.swing.Icon;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Victor
 */
public enum IssueType implements Classifier {
    
    BUG,
    VULNERABILITY,
    CODE_SMELL;

    @Override
    public IssueFilter createFilter() {
        throw new UnsupportedOperationException();
//        return new IssueTypeFilter(this);
    }

    @Override
    public Icon getIcon() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getUserDescription() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
