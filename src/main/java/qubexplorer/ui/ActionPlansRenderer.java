package qubexplorer.ui;

import java.awt.Component;
import java.text.DateFormat;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import org.sonar.wsclient.issue.ActionPlan;

/**
 *
 * @author Victor
 */
public class ActionPlansRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
        if (value instanceof ActionPlan) {
            ActionPlan actionPlan = (ActionPlan) value;
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
            label.setText(actionPlan.name() + " - " + dateFormat.format((actionPlan).deadLine()));
        }
        return label;
    }

}
