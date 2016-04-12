package qubexplorer.ui.summary;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;
import qubexplorer.Summary;
import qubexplorer.filter.IssueFilter;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.ActionPlansTask;
import qubexplorer.ui.ProjectChooser;
import qubexplorer.ui.ProjectContext;
import qubexplorer.ui.SonarQubeOptionsPanel;
import qubexplorer.ui.task.TaskExecutor;

@ActionID(
        category = "SonarQube",
        id = "qubexplorer.ui.SonarDialogAction2")
@ActionRegistration(
        displayName = "#CTL_SonarDialogAction2")
@Messages("CTL_SonarDialogAction2=Get Issues from Server ...")
public final class CustomServerIssuesAction implements ActionListener {

    private final Project context;

    public CustomServerIssuesAction(Project context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        ProjectChooser chooser=new ProjectChooser(WindowManager.getDefault().getMainWindow(), true);
        chooser.setSelectedUrl(NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000"));
        if(chooser.showDialog() == ProjectChooser.Option.ACCEPT) {
            final ProjectContext projectContext = new ProjectContext(context, chooser.getSelectedProject());
            final SonarQube sonarQube = new SonarQube(chooser.getSelectedUrl());
            TaskExecutor.execute(new SummaryTask(sonarQube, projectContext, new IssueFilter[0]){

                @Override
                protected void success(Summary summary) {
                    super.success(summary);
                    TaskExecutor.execute(new ActionPlansTask(sonarQube, projectContext));
                }
                
            });
        }
    }
    
}
