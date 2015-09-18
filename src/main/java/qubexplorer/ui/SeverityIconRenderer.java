package qubexplorer.ui;

import java.awt.Component;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import qubexplorer.Severity;

/**
 *
 * @author Victor
 */
public class SeverityIconRenderer extends DefaultTableCellRenderer{
    private final Map<Severity, Icon> iconsBySeverity=new EnumMap<>(Severity.class);

    public SeverityIconRenderer() {
        iconsBySeverity.put(Severity.BLOCKER, new ImageIcon(getClass().getResource("/qubexplorer/ui/images/blocker.png")));
        iconsBySeverity.put(Severity.CRITICAL, new ImageIcon(getClass().getResource("/qubexplorer/ui/images/critical.png")));
        iconsBySeverity.put(Severity.MAJOR, new ImageIcon(getClass().getResource("/qubexplorer/ui/images/major.png")));
        iconsBySeverity.put(Severity.MINOR, new ImageIcon(getClass().getResource("/qubexplorer/ui/images/minor.png")));
        iconsBySeverity.put(Severity.INFO, new ImageIcon(getClass().getResource("/qubexplorer/ui/images/info.png")));
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1) {
        JLabel component = (JLabel) super.getTableCellRendererComponent(jtable, o, bln, bln1, i, i1);
        component.setText(null);
        component.setIcon(iconsBySeverity.get((Severity)o));
        component.setIconTextGap(0);
        component.setBorder(null);
        return component; 
    }
    
}
