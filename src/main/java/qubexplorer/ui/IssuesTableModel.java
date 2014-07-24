package qubexplorer.ui;

import javax.swing.table.DefaultTableModel;
import qubexplorer.RadarIssue;
import qubexplorer.Severity;

/**
 *
 * @author Victor
 */
public class IssuesTableModel extends DefaultTableModel {
    private RadarIssue[] issues;
    
    private final Class[] types = new Class[]{
        Severity.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class, Severity.class, java.lang.String.class
    };

    public IssuesTableModel() {
        super(new Object [][] {}, new String [] {"", "Location", "Message", "Rule", "Severity", "Project Key"});
    }
    
    public void add(RadarIssue issue) {
        int lineNumber=issue.line() == null ? 0: issue.line();
        IssueLocation issueLocation = new IssueLocation(issue.componentKey(), lineNumber);
        addRow(new Object[]{issue.severityObject(), issueLocation, issue.message(), issue.rule().getTitle(), issue.severityObject(), issueLocation.getProjectKey()});
    }
    
    public void setIssues(RadarIssue[] issues) {
        this.issues=issues;
        //TODO call setData
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
        return types[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }
    
}

