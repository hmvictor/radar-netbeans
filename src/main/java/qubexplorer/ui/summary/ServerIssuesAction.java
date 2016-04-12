package qubexplorer.ui.summary;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import qubexplorer.SonarQubeProjectBuilder;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.Summary;
import qubexplorer.filter.IssueFilter;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.ActionPlansTask;
import qubexplorer.ui.ProjectContext;
import qubexplorer.ui.SonarQubeFactory;
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
        SonarQubeProjectConfiguration configuration = SonarQubeProjectBuilder.getDefaultConfiguration(context);
        if (configuration != null) {
            final ProjectContext projectContext = new ProjectContext(context, configuration);
            final SonarQube sonarQube = SonarQubeFactory.createForDefaultServerUrl();
            TaskExecutor.execute(new SummaryTask(sonarQube, projectContext, new IssueFilter[0]) {

                @Override
                protected void success(Summary summary) {
                    super.success(summary);
                    TaskExecutor.execute(new ActionPlansTask(sonarQube, projectContext));
                }

            });
        }
    }

}
