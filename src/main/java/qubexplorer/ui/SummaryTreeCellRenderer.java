package qubexplorer.ui;

import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import qubexplorer.Severity;

/**
 *
 * @author Victor
 */
public class SummaryTreeCellRenderer extends DefaultTableCellRenderer{

    @Override
    public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1) {
        System.out.println("getRenderer");
        System.out.println(o.getClass());
        if(o instanceof Severity) {
            System.out.println("set icon");
            setIcon(new ImageIcon(getClass().getResource("/qubexplorer/ui/images/"+o.toString().toLowerCase()+".png")));
        }
        setText(o.toString());
        return this;
    }
    
    
}
