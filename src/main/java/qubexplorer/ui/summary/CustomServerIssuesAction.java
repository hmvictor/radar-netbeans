package qubexplorer.ui.summary;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;
import qubexplorer.ConfigurationFactory;
import qubexplorer.ResourceKey;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.Summary;
import qubexplorer.UserCredentials;
import qubexplorer.filter.IssueFilter;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.ActionPlansTask;
import qubexplorer.ui.ProjectContext;
import qubexplorer.ui.ServerConnectionDialog;
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
//        ProjectChooser chooser=new ProjectChooser(WindowManager.getDefault().getMainWindow(), true);
        ServerConnectionDialog serverConnectionDialog=new ServerConnectionDialog(WindowManager.getDefault().getMainWindow(), true);
        serverConnectionDialog.setSelectedUrl(NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000"));
        if(serverConnectionDialog.showDialog() == ServerConnectionDialog.Option.ACCEPT) {
            SonarQubeProjectConfiguration fixed = serverConnectionDialog.getSelectedProject();
            SonarQubeProjectConfiguration real = ConfigurationFactory.createDefaultConfiguration(context);
            final ProjectContext projectContext = new ProjectContext(context, new FixedKey(fixed, real));
            final SonarQube sonarQube = new SonarQube(serverConnectionDialog.getSelectedUrl());
            SummaryTask summaryTask = new SummaryTask(sonarQube, projectContext, new IssueFilter[0]){
                
                @Override
                protected void success(Summary summary) {
                    super.success(summary);
                    TaskExecutor.execute(new ActionPlansTask(sonarQube, projectContext));
                }
                
            };
            UserCredentials userCredentials = serverConnectionDialog.getUserCredentials();
            if(userCredentials != null) {
                summaryTask.setUserCredentials(userCredentials);
            }
            TaskExecutor.execute(summaryTask);
        }
    }
    
    public static class FixedKey implements SonarQubeProjectConfiguration{
        private final SonarQubeProjectConfiguration fixed;
        private final SonarQubeProjectConfiguration real;

        public FixedKey(SonarQubeProjectConfiguration fixed, SonarQubeProjectConfiguration real) {
            this.fixed = fixed;
            this.real = real;
        }
        
        @Override
        public String getName() {
            return fixed.getName();
        }

        @Override
        public ResourceKey getKey() {
            return fixed.getKey();
        }

        @Override
        public String getVersion() {
            return fixed.getVersion();
        }

        @Override
        public SonarQubeProjectConfiguration createConfiguration(Project subproject) {
            return real.createConfiguration(subproject);
        }

        @Override
        public Properties getProperties() {
            return real.getProperties();
        }
        
    }
    
}
