package qubexplorer.ui.summary;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import qubexplorer.ConfigurationFactory;
import qubexplorer.ui.ProjectContext;
import qubexplorer.ui.SonarQubeOptionsPanel;
import qubexplorer.ui.task.TaskExecutor;

/**
 *
 * @author Victor
 */
@ActionID(
        category = "SonarQube",
        id = "qubexplorer.ui.SonarRunnerAction")
@ActionRegistration(
        displayName = "#CTL_SonarRunnerAction")
@NbBundle.Messages("CTL_SonarRunnerAction=Get Issues with Sonar Runner")
public class SonarRunnerAction implements ActionListener {

    private final Project context;

    public SonarRunnerAction(Project context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        String serverUrl = NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000");
        TaskExecutor.execute(new SonarRunnerTask(new ProjectContext(context, ConfigurationFactory.createDefaultConfiguration(context)), serverUrl));
    }

}
