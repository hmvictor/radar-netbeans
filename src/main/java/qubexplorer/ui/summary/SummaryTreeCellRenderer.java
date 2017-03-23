package qubexplorer.ui.summary;

import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import qubexplorer.Classifier;
import qubexplorer.ClassifierSummary;
import qubexplorer.Rule;
import qubexplorer.Severity;

/**
 * Render in the tree table
 * 
 * @author Victor
 */
public class SummaryTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree jtree, Object o, boolean bln, boolean bln1, boolean bln2, int i, boolean bln3) {
        Component c = super.getTreeCellRendererComponent(jtree, o, bln, bln1, bln2, i, bln3);
        if (o instanceof Severity) {
            setText(((Classifier)o).getUserDescription());
            setIcon(((Classifier)o).getIcon());
        }else if(o instanceof Rule) {
            setIcon(null);
            setText(((Rule)o).getName());
        }else if(o instanceof ClassifierSummary){
            setIcon(null);
            setText("Issues");
        }
        return c;
    }

}
