package qubexplorer.ui.summary;

import java.awt.Component;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import org.sonar.wsclient.services.Rule;
import qubexplorer.Severity;
import qubexplorer.Summary;

/**
 *
 * @author Victor
 */
public class SummaryTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final Map<Severity, String> TEXT_BY_SEVERITY;
    
    static {
        TEXT_BY_SEVERITY=new EnumMap<>(Severity.class);
        TEXT_BY_SEVERITY.put(Severity.BLOCKER, "Blocker");
        TEXT_BY_SEVERITY.put(Severity.CRITICAL, "Critical");
        TEXT_BY_SEVERITY.put(Severity.MAJOR, "Major");
        TEXT_BY_SEVERITY.put(Severity.MINOR, "Minor");
        TEXT_BY_SEVERITY.put(Severity.INFO, "Info");
    }

    @Override
    public Component getTreeCellRendererComponent(JTree jtree, Object o, boolean bln, boolean bln1, boolean bln2, int i, boolean bln3) {
        Component c = super.getTreeCellRendererComponent(jtree, o, bln, bln1, bln2, i, bln3);
        if (o instanceof Severity) {
            setText(TEXT_BY_SEVERITY.get((Severity)o));
            setIcon(new ImageIcon(getClass().getResource("/qubexplorer/ui/images/" + o.toString().toLowerCase() + ".png")));
        }else if(o instanceof Rule) {
            setIcon(null);
            setText(((Rule)o).getTitle());
        }else if(o instanceof Summary){
            setIcon(null);
            setText("Issues");
        }
        return c;
    }

}
