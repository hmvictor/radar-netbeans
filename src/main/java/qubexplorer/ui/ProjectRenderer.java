package qubexplorer.ui;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import qubexplorer.server.SonarProject;

/**
 *
 * @author Victor
 */
public class ProjectRenderer extends DefaultListCellRenderer{
    
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
        if (value instanceof SonarProject) {
            SonarProject project=(SonarProject) value;
            label.setText(String.format("%s (%s)", project.getName(), project.getKey()));
        }
        return label;
    }
    
}
