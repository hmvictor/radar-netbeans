package qubexplorer.ui;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import qubexplorer.SonarQubeProjectConfiguration;

/**
 *
 * @author Victor
 */
public class ProjectRenderer extends DefaultListCellRenderer{
    
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
        if (value instanceof SonarQubeProjectConfiguration) {
            SonarQubeProjectConfiguration project=(SonarQubeProjectConfiguration) value;
            label.setText(toString(project));
        }
        return label;
    }
    
    public static String toString(SonarQubeProjectConfiguration project){
        return String.format("%s (%s)", project.getName(), project.getKey());
    }
    
}
