/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package qubexplorer.ui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.services.Rule;
import qubexplorer.Counting;
import qubexplorer.Severity;
import qubexplorer.SonarQube;
import qubexplorer.info.SonarMainTopComponent;
import qubexplorer.ui.options.SonarQubePanel;

@ActionID(
        category = "Build",
        id = "qubexplorer.ui.SonarDialogAction")
@ActionRegistration(
        displayName = "#CTL_SonarDialogAction")
@Messages("CTL_SonarDialogAction=Sonar")
@ActionReferences(value={
@ActionReference(path="Projects/Actions"),
@ActionReference(path = "Menu/Source", position = 8962, separatorBefore = 8956, separatorAfter = 8968)})
public final class SonarDialogAction implements ActionListener {

    private final Project context;

    public SonarDialogAction(Project context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        CountsWorker worker=new CountsWorker(context);
        worker.execute();
        
//        Frame frame = WindowManager.getDefault().getMainWindow();
//        SonarQubeDialog dialog = new SonarQubeDialog(frame, true, context);
//        dialog.setLocationRelativeTo(frame);
//        dialog.setVisible(true);
    }
    
    private class CountsWorker extends SwingWorker<Counting, Void>{
        private ProgressHandle handle;
        private Project project;

        public CountsWorker(Project project) {
            this.project=project;
            handle = ProgressHandleFactory.createHandle("Sonar");
            handle.switchToIndeterminate();
            handle.start();
        }
        
        @Override
        protected Counting doInBackground() throws Exception {
            Counting counting=new Counting();
            Map<Rule, Integer> counts=new HashMap<>();
//            counts.put("X", 10);
//            counts.put("Y", 10);
            counting.setRuleCounts(Severity.BLOCKER, counts);
//            return counting;
            return new SonarQube(NbPreferences.forModule(SonarQubePanel.class).get("address", "http://localhost:9000")).getCounting(SonarQube.toResource(project));
        }

        @Override
        protected void done() {
            try {
                Counting counting = get();
                SonarMainTopComponent infoTopComponent = (SonarMainTopComponent) WindowManager.getDefault().findTopComponent("InfoTopComponent");
                infoTopComponent.setProject(project);
                infoTopComponent.setCounting(counting);
                infoTopComponent.open();
                infoTopComponent.requestVisible();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ExecutionException ex) {
                Exceptions.printStackTrace(ex);
            } finally{
                handle.finish();
            }
        }
        
    }
    
}
