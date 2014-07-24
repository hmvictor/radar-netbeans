package qubexplorer.ui;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author Victor
 */
public class LocationRenderer extends DefaultTableCellRenderer{

    @Override
    public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1) {
        Object value;
        if(o instanceof IssueLocation){
            IssueLocation location = (IssueLocation)o;
            if(location.getLineNumber() > 0){
                value=String.format("%s [%d]", location.getPath(), location.getLineNumber());
            }else{
                value=location.getPath();
            }
        }else{
            value=o;
        }
        return super.getTableCellRendererComponent(jtable, value, bln, bln1, i, i1); //To change body of generated methods, choose Tools | Templates.
    }
    
}
