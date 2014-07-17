package qubexplorer.ui;

import java.awt.Component;
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

    @Override
    public Component getTreeCellRendererComponent(JTree jtree, Object o, boolean bln, boolean bln1, boolean bln2, int i, boolean bln3) {
        Component c = super.getTreeCellRendererComponent(jtree, o, bln, bln1, bln2, i, bln3);
        if (o instanceof Severity) {
            switch((Severity)o){
                case BLOCKER:
                    setText("Blocker");
                    break;
                case CRITICAL:
                    setText("Critical");
                    break;
                case MAJOR:
                    setText("Major");
                    break;
                case MINOR:
                    setText("Minor");
                    break;
                case INFO:
                    setText("Info");
                    break;
            }
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
