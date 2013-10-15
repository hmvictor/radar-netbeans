package qubexplorer.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Build",
        id = "qubexplorer.ui.SonarDialogAction")
@ActionRegistration(
        displayName = "#CTL_SonarDialogAction")
@Messages("CTL_SonarDialogAction=Sonar")
@ActionReferences(value={
@ActionReference(path="Projects/Actions"),
@ActionReference(path = "Menu/Source", position = 8962, separatorBefore = 8956, separatorAfter = 8968)})
public final class SonarQubeAction implements ActionListener {

    private final Project context;

    public SonarQubeAction(Project context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        CountsWorker worker=new CountsWorker(context);
        worker.execute();
    }
    
}
