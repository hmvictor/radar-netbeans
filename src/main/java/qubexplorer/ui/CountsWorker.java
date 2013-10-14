package qubexplorer.ui;

import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.base.HttpException;
import qubexplorer.Authentication;
import qubexplorer.AuthenticationRepository;
import qubexplorer.Counting;
import qubexplorer.SonarQube;
import qubexplorer.info.SonarMainTopComponent;
import qubexplorer.ui.options.SonarQubePanel;

/**
 *
 * @author Victor
 */
class CountsWorker extends SwingWorker<Counting, Void> {
    private ProgressHandle handle;
    private Project project;
    private Authentication auth;

    public CountsWorker(Project project) {
        this.project = project;
        init();
    }

    public CountsWorker(Authentication auth, Project project) {
        this.auth = auth;
        this.project = project;
        init();
    }

    @Override
    protected Counting doInBackground() throws Exception {
        return new SonarQube(NbPreferences.forModule(SonarQubePanel.class).get("address", "http://localhost:9000")).getCounting(auth, SonarQube.toResource(project));
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
            if (ex.getCause() instanceof HttpException) {
                if (((HttpException) ex.getCause()).status() == 401) {
                    if(auth != null) {
                        AuthenticationRepository.getInstance().invalidateAuthentication();
                    }
                    handle.finish();
                    Authentication authentication = AuthenticationRepository.getInstance().getAuthentication();
                    if (authentication != null) {
                        CountsWorker worker = new CountsWorker(authentication, project);
                        worker.execute();
                    }
                }
            } else {
                Exceptions.printStackTrace(ex);
            }
        } finally {
            handle.finish();
        }
    }

    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.switchToIndeterminate();
        handle.start();
    }
    
}
