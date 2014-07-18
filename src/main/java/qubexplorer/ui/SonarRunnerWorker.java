package qubexplorer.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.openide.util.Exceptions;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.WindowManager;
import org.sonar.runner.api.PrintStreamConsumer;
import qubexplorer.runner.SonarRunnerProccess;
import qubexplorer.runner.SonarRunnerResult;

/**
 *
 * @author Victor TODO: process result
 */
public class SonarRunnerWorker extends UITask<SonarRunnerResult, Void> {

    private Project project;
    private String sonarUrl;
    private ProgressHandle handle;
    private InputOutput io;

    public SonarRunnerWorker(Project project, String sonarUrl) {
        this.project = project;
        this.sonarUrl = sonarUrl;
        init();
    }

    private void init() {
        handle = ProgressHandleFactory.createHandle("Sonar-runner");
        handle.start();
        handle.switchToIndeterminate();
        io = IOProvider.getDefault().getIO("Sonar-runner", false);
        try {
            io.getOut().reset();
            io.getErr().reset();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        io.select();
        io.getOut().println("Starting sonar-runner");
    }

    @Override
    protected SonarRunnerResult doInBackground() throws Exception {
        PrintStreamConsumer out = new PrintStreamConsumer(null){

            @Override
            public void consumeLine(String line) {
                io.getOut().println(line);
            }
            
        };
        
        PrintStreamConsumer err = new PrintStreamConsumer(null){

            @Override
            public void consumeLine(String line) {
                io.getErr().println(line);
            }
            
        };
        SonarRunnerProccess sonarRunnerProccess = new SonarRunnerProccess(sonarUrl, project);
        sonarRunnerProccess.setOutConsumer(out);
        sonarRunnerProccess.setErrConsumer(err);
        return sonarRunnerProccess.executeRunner();
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
        io.getOut().close();
        io.getErr().close();
    }

}
