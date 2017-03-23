package qubexplorer.server.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import qubexplorer.ConfigurationFactory;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.ProjectContext;
import qubexplorer.ui.summary.SummaryTask;
import qubexplorer.ui.task.TaskExecutor;

@ActionID(
        category = "SonarQube",
        id = "qubexplorer.ui.SonarDialogAction")
@ActionRegistration(
        displayName = "#CTL_SonarDialogAction")
@Messages("CTL_SonarDialogAction=Get Issues from Server")
public final class ServerIssuesAction implements ActionListener {

    private final Project context;

    public ServerIssuesAction(Project context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        SonarQubeProjectConfiguration configuration = ConfigurationFactory.createDefaultConfiguration(context);
        if (configuration != null) {
            final ProjectContext projectContext = new ProjectContext(context, configuration);
            final SonarQube sonarQube = SonarQubeFactory.createForDefaultServerUrl();
            TaskExecutor.execute(new SummaryTask(sonarQube, projectContext, Collections.emptyList()));
        }
    }

}
