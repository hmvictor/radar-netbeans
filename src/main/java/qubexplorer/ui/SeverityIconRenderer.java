package qubexplorer.ui;

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import qubexplorer.Classifier;

/**
 * Render in the issues table.
 * 
 * @author Victor
 */
public class SeverityIconRenderer extends DefaultTableCellRenderer{
    
    @Override
    public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1) {
        JLabel component = (JLabel) super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
        component.setText(null);
        component.setIcon(((Classifier)o).getIcon());
        component.setIconTextGap(0);
        component.setBorder(null);
        return component; 
    }
    
}
