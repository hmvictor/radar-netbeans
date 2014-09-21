package qubexplorer.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import qubexplorer.ui.options.SonarQubeOptionsPanel;
import qubexplorer.ui.task.SonarRunnerTask;
import qubexplorer.ui.task.TaskExecutor;

/**
 *
 * @author Victor TODO: register and customize
 */
@ActionID(
        category = "Build",
        id = "qubexplorer.ui.SonarRunnerAction")
@ActionRegistration(
        displayName = "#CTL_SonarRunnerAction")
@NbBundle.Messages("CTL_SonarRunnerAction=Get Issues with Sonar Runner")
@ActionReferences(value = {
    @ActionReference(path = "Projects/Actions", position = 8964, separatorBefore = 8956, separatorAfter = 8968),
    @ActionReference(path = "Menu/Source", position = 8964, separatorBefore = 8956, separatorAfter = 8968)})
public class SonarRunnerAction implements ActionListener {

    private final Project context;

    public SonarRunnerAction(Project context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String serverUrl = NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000");
//            new SonarRunnerWorker(context, serverUrl).execute();
        TaskExecutor.execute(new SonarRunnerTask(new ProjectContext(context), serverUrl));
    }

}
