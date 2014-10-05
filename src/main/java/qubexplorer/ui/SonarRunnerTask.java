package qubexplorer.ui;

import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.windows.IOContainer;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.WindowManager;
import org.sonar.runner.api.PrintStreamConsumer;
import org.sonar.runner.api.ProcessMonitor;
import qubexplorer.runner.SonarRunnerCancelledException;
import qubexplorer.runner.SonarRunnerProccess;
import qubexplorer.runner.SonarRunnerResult;
import qubexplorer.runner.SourcesNotFoundException;
import qubexplorer.ui.options.SonarQubeOptionsPanel;
import qubexplorer.ui.task.Task;

/**
 *
 * @author Victor
 */
public class SonarRunnerTask extends Task<SonarRunnerResult>{
    private InputOutput io;
    private boolean stopped=false;
    private final Action stopAction=new AbstractAction("Stop", new ImageIcon(getClass().getResource("/qubexplorer/ui/images/stop.png"))) {
        
        @Override
        public void actionPerformed(ActionEvent ae) {
            stopped=true;
            setEnabled(false);
        }
        
    };

    public SonarRunnerTask(ProjectContext projectContext, String serverUrl) {
        super(projectContext, serverUrl);
    }
    
    @Override
    protected void init() {
        stopAction.setEnabled(true);
        if(io == null) {
            io = IOProvider.getDefault().getIO("Sonar-runner", true, new Action[]{stopAction}, IOContainer.getDefault());
        }
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
    public SonarRunnerResult execute() throws Exception {
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
        SonarRunnerProccess sonarRunnerProccess = new SonarRunnerProccess(getServerUrl(), getProjectContext().getProject());
        sonarRunnerProccess.setAnalysisMode(SonarRunnerProccess.AnalysisMode.valueOf(NbPreferences.forModule(SonarQubeOptionsPanel.class).get("runner.analysisMode", "Preview").toUpperCase()));
        sonarRunnerProccess.setOutConsumer(out);
        sonarRunnerProccess.setErrConsumer(err);
        return sonarRunnerProccess.executeRunner(getUserCredentials(), new ProcessMonitor() {

            @Override
            public boolean stop() {
                return stopped;
            }
            
        });
    }

    @Override
    protected void success(SonarRunnerResult result) {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        sonarTopComponent.setProjectContext(getProjectContext());
        sonarTopComponent.setIssuesContainer(result);
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        sonarTopComponent.showSummary(result.getSummary());
    }

    @Override
    protected void fail(Throwable cause) {
        if(cause instanceof SourcesNotFoundException) {
            String message = org.openide.util.NbBundle.getMessage(SonarRunnerTask.class, "SourcesNotFound");
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(message, NotifyDescriptor.WARNING_MESSAGE));
        }else if(cause instanceof SonarRunnerCancelledException){
            io.getOut().println(org.openide.util.NbBundle.getMessage(SonarRunnerTask.class, "SonarRunner.cancelled"));
        }else{
            io.getErr().println(org.openide.util.NbBundle.getMessage(SonarRunnerTask.class, "SonarRunner.error"));
            Exceptions.printStackTrace(cause);
        }
    }

    @Override
    protected void destroy() {
        stopAction.setEnabled(true);
        io.getOut().close();
        io.getErr().close();
    }
    
}
