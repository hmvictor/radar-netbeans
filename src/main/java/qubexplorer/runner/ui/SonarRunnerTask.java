package qubexplorer.runner.ui;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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
import qubexplorer.MvnModelInputException;
import qubexplorer.ResourceKey;
import qubexplorer.Severity;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.SummaryOptions;
import qubexplorer.runner.SonarRunnerCancelledException;
import qubexplorer.runner.SonarRunnerProccess;
import qubexplorer.runner.SonarRunnerResult;
import qubexplorer.runner.SourcesNotFoundException;
import qubexplorer.ui.ProjectContext;
import qubexplorer.ui.SonarIssuesTopComponent;
import qubexplorer.ui.SonarQubeOptionsPanel;
import qubexplorer.ui.issues.IssueLocation;
import qubexplorer.ui.task.Task;
import qubexplorer.ui.task.TaskExecutionException;

/**
 *
 * @author Victor
 */
public class SonarRunnerTask extends Task<SonarRunnerResult>{
    private InputOutput io;
    private boolean stopped=false;
    private final Action stopAction=new AbstractAction("Stop Sonar-runner", new ImageIcon(getClass().getResource("/qubexplorer/ui/images/stop.png"))) {
        
        {
            putValue(Action.SHORT_DESCRIPTION, "Stops sonar-runner");
        }
        
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
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        sonarTopComponent.setSummaryOptions(new SummaryOptions<>(Severity.getType(), Collections.emptyList()));
        sonarTopComponent.resetState();
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
    public SonarRunnerResult execute() throws TaskExecutionException {
        try {
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
            SonarRunnerProccess sonarRunnerProccess = new SonarRunnerProccess(getServerUrl(), getProjectContext());
            SonarRunnerProccess.AnalysisMode defaulAnalysisMode = SonarRunnerProccess.getDefaultAnalysisMode();
            String preferedAnalysisMode = NbPreferences.forModule(SonarQubeOptionsPanel.class).get("runner.analysisMode", defaulAnalysisMode.getFriendlyName());
            try{
                sonarRunnerProccess.setAnalysisMode(SonarRunnerProccess.AnalysisMode.valueOfFriendlyName(preferedAnalysisMode));
            }catch(IllegalArgumentException ex) {
                sonarRunnerProccess.setAnalysisMode(defaulAnalysisMode);
            }
            String jvmArguments = NbPreferences.forModule(SonarQubeOptionsPanel.class).get("runner.jvmArguments", "");
            sonarRunnerProccess.setJvmArguments(Arrays.asList(jvmArguments.split(" +")));
            sonarRunnerProccess.setOutConsumer(out);
            sonarRunnerProccess.setErrConsumer(err);
            return sonarRunnerProccess.executeRunner(getUserCredentials(), () -> stopped);
        } catch (MvnModelInputException ex) {
            throw new TaskExecutionException(ex);
        }
    }

    @Override
    protected void success(SonarRunnerResult result) {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        sonarTopComponent.setProjectContext(getProjectContext());
        sonarTopComponent.setProjectKeyChecker(new SonarRunnerChecker());
        sonarTopComponent.setIssuesContainer(result);
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        SummaryOptions summaryOptions=new SummaryOptions(Severity.getType(), Collections.emptyList());
        sonarTopComponent.showSummary(summaryOptions, result.getClassifierSummaryBySeverity());
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
        stopAction.setEnabled(false);
        io.getOut().close();
        io.getErr().close();
    }
    
    private static class SonarRunnerChecker implements IssueLocation.ProjectKeyChecker {

        public ResourceKey getShortProjectKey(ResourceKey key) {
            return key.subkey(1, key.getPartsCount());
        }

        @Override
        public boolean equals(SonarQubeProjectConfiguration configuration, ResourceKey projectKeyIssue, boolean isSubmodule) {
            ResourceKey tmpKey = isSubmodule ? getShortProjectKey(projectKeyIssue) : projectKeyIssue;
            return configuration.getKey().equals(tmpKey);
        }

    }
    
}
