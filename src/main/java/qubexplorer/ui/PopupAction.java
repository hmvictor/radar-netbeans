package qubexplorer.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

@ActionID(
        category = "Build",
        id = "qubexplorer.ui.SonarQube"
)
@ActionRegistration(
        displayName = "#CTL_SonarQubePopupAction", lazy = false
)
@ActionReferences({
    @ActionReference(path = "Projects/Actions")
//    @ActionReference(path = "Menu/Source", position = 8964, separatorBefore = 8956, separatorAfter = 8968)
})
@Messages("CTL_SonarQubePopupAction=SonarQube")
public final class PopupAction extends AbstractAction implements ActionListener, Presenter.Popup {
 
    @Override
    public void actionPerformed(ActionEvent e) {
        /* No need to do something. Required method below. */
    }
 
    @Override
    public JMenuItem getPopupPresenter() {
        JMenu main = new JMenu(Bundle.CTL_SonarQubePopupAction());
        List<? extends Action> actionsForPath = Utilities.actionsForPath("Actions/SonarQube");
        for (Action action : actionsForPath) {
            main.add(action);
        }
        return main;
    }
    
}