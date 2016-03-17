package qubexplorer.ui;

import javax.swing.table.DefaultTableModel;
import qubexplorer.RadarIssue;
import qubexplorer.Severity;

/**
 *
 * @author Victor
 */
public class IssuesTableModel extends DefaultTableModel {
    private static final Class[] COLUMN_TYPES = new Class[]{
        Severity.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class, Severity.class, java.lang.String.class, String.class
    };
    
    private static final String[] COLUMN_NAMES = new String [] {"", "Location", "Message", "Rule", "Severity", "Project Key", "Full Path"};
    
    private transient RadarIssue[] issues;

    public IssuesTableModel() {
        super(new Object [][] {}, COLUMN_NAMES);
    }
    
    
    public void add(RadarIssue issue) {
        addRow(createRowData(issue));
    }
    
    public Object[] createRowData(RadarIssue issue){
        IssueLocation issueLocation=issue.getLocation();
        return new Object[]{issue.severityObject(), issueLocation, issue.message(), issue.rule().getTitle(), issue.severityObject(), issueLocation.getProjectKey(), issueLocation.getPath()};
    }
    
    public void setIssues(RadarIssue[] issues) {
        this.issues=issues;
        while (getRowCount() > 0) {
            removeRow(0);
        }
        for (RadarIssue issue : issues) {
            add(issue);
        }
    }
    
    public RadarIssue getIssue(int row) {
        return issues[row];
    }
    
    public IssueLocation getIssueLocation(int row) {
        return (IssueLocation) getValueAt(row, 1);
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        return COLUMN_TYPES[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
    
}

