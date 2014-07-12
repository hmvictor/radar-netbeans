package qubexplorer.ui;

import java.util.List;
import org.netbeans.api.project.Project;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.issue.Issue;
import qubexplorer.IssueDecorator;
import qubexplorer.filter.IssueFilter;
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

    public SonarRunnerWorker(Project project, String sonarUrl) {
        this.project = project;
        this.sonarUrl = sonarUrl;
    }
    
    @Override
    protected SonarRunnerResult doInBackground() throws Exception {
        return new SonarRunnerProccess(sonarUrl, project).executeRunner();
    }

    @Override
    protected void success(SonarRunnerResult result) {
        List<Issue> issues = result.getIssues(null, null);
        for(Issue issue:issues) {
            System.out.println(issue.message());
        }
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarTopComponent");
        sonarTopComponent.setIssues(new IssueFilter[0]);
        sonarTopComponent.setProject(project);
        try{
            sonarTopComponent.setSummary(result.getSummary());
        }catch(Exception ex) {
            ex.printStackTrace();
        }
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
    }

    @Override
    protected void error(Throwable cause) {
        cause.printStackTrace();
    }
    
}
