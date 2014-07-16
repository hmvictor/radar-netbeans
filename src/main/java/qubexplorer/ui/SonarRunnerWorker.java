package qubexplorer.ui;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.windows.WindowManager;
import qubexplorer.runner.SonarRunnerProccess;
import qubexplorer.runner.SonarRunnerResult;

/**
 *
 * @author Victor
 * TODO: process result
 */
public class SonarRunnerWorker extends UITask<SonarRunnerResult, Void>{
    private Project project;
    private String sonarUrl;
    private ProgressHandle handle;

    public SonarRunnerWorker(Project project, String sonarUrl) {
        this.project = project;
        this.sonarUrl = sonarUrl;
        init();
    }
    
    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar-runner");
        handle.start();
        handle.switchToIndeterminate();
    }
    
    @Override
    protected SonarRunnerResult doInBackground() throws Exception {
        return new SonarRunnerProccess(sonarUrl, project).executeRunner();
    }

    @Override
    protected void success(SonarRunnerResult result) {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        sonarTopComponent.setProject(project);
        sonarTopComponent.setSummary(result.getSummary());
        sonarTopComponent.setIssuesContainer(result);
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        sonarTopComponent.showSummary();
    }

    @Override
    protected void error(Throwable cause) {
        cause.printStackTrace();
    }

    @Override
    protected void finished() {
        handle.finish();
    }
    
}
