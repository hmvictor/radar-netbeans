package qubexplorer.ui;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;
import qubexplorer.Counting;
import qubexplorer.NoSuchProjectException;
import qubexplorer.SonarQube;
import qubexplorer.ui.options.SonarQubeOptionsPanel;

/**
 *
 * @author Victor
 */
class CountsWorker2 extends SonarQubeWorker<Counting, Void> {
    private ProgressHandle handle;
    private Project project;

    public CountsWorker2(Project project, String url, String resource) {
        super(url, resource);
        this.project = project;
        init();
    }
    
    @Override
    protected Counting doInBackground() throws Exception {
        return new SonarQube(getServerUrl()).getCounting(getAuthentication(), getResourceKey());
    }

    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar");
        handle.switchToIndeterminate();
        handle.start();
    }

    @Override
    protected void success(Counting counting) {
        SonarMainTopComponent infoTopComponent = (SonarMainTopComponent) WindowManager.getDefault().findTopComponent("InfoTopComponent");
        infoTopComponent.setProject(project);
        infoTopComponent.setCounting(counting);
        infoTopComponent.setSonarQubeUrl(getServerUrl());
        infoTopComponent.setResourceKey(getResourceKey());
        infoTopComponent.open();
        infoTopComponent.requestVisible();
    }

    @Override
    protected void finished() {
        handle.finish();
    }

    @Override
    protected SonarQubeWorker createCopy() {
        return new CountsWorker2(project, getServerUrl(), getResourceKey());
    }

    @Override
    protected void error(Throwable cause) {
        if(cause instanceof NoSuchProjectException) {
            if(getAuthentication() != null) {
                AuthenticationRepository.getInstance().saveAuthentication(getServerUrl(), null, getAuthentication());
            }
            ProjectChooser chooser=new ProjectChooser(WindowManager.getDefault().getMainWindow(), true);
            chooser.setSelectedUrl(NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000"));
            chooser.setServerUrlEnabled(false);
            chooser.loadProjectKeys();
            if(chooser.showDialog() == ProjectChooser.Option.ACCEPT) {
                scheduleWorker(new CountsWorker2(project, getServerUrl(), chooser.getSelectedProjectKey()));
            }
        }else{
            super.error(cause);
        }
    }
    
}
