package qubexplorer.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;
import qubexplorer.ui.options.SonarQubeOptionsPanel;

@ActionID(
        category = "Build",
        id = "qubexplorer.ui.SonarDialogAction2")
@ActionRegistration(
        displayName = "#CTL_SonarDialogAction2")
@Messages("CTL_SonarDialogAction2=Custom Sonar Connection ...")
@ActionReferences(value={
@ActionReference(path="Projects/Actions"),
@ActionReference(path = "Menu/Source", position = 8962, separatorBefore = 8956, separatorAfter = 8968)})
public final class SonarQubeAction2 implements ActionListener {

    private final Project context;

    public SonarQubeAction2(Project context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        ProjectChooser chooser=new ProjectChooser(WindowManager.getDefault().getMainWindow(), true);
        chooser.setSelectedUrl(NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000"));
        if(chooser.showDialog() == ProjectChooser.Option.ACCEPT) {
            CountsWorker worker=new CountsWorker(context, chooser.getSelectedUrl(), chooser.getSelectedProjectKey());
            worker.execute();
        }
    }
    
}
