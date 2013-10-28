package qubexplorer.ui;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.base.HttpException;
import qubexplorer.Authentication;
import qubexplorer.Counting;
import qubexplorer.SonarQube;
import qubexplorer.ui.options.SonarQubeOptionsPanel;

/**
 *
 * @author Victor
 */
class CountsWorker extends SwingWorker<Counting, Void> {
    private ProgressHandle handle;
    private Project project;
    private Authentication auth;
    private String url;
    private String resource;

    public CountsWorker(Project project) throws IOException, XmlPullParserException {
        this(project, NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000"), SonarQube.toResource(project));
    }

    public CountsWorker(Project project, String url, String resource) {
        this.project = project;
        this.url = url;
        this.resource = resource;
        init();
    }
    
    public void setAuth(Authentication auth) {
        this.auth = auth;
    }

    @Override
    protected Counting doInBackground() throws Exception {
        return new SonarQube(url).getCounting(auth, resource);
    }

    @Override
    protected void done() {
        try {
            Counting counting = get();
            SonarMainTopComponent infoTopComponent = (SonarMainTopComponent) WindowManager.getDefault().findTopComponent("InfoTopComponent");
            infoTopComponent.setProject(project);
            infoTopComponent.setCounting(counting);
            infoTopComponent.setSonarQubeUrl(url);
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
                        CountsWorker worker = new CountsWorker(project, url, resource);
                        worker.setAuth(auth);
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
