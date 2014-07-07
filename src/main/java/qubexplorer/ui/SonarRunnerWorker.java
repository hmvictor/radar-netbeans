package qubexplorer.ui;

import java.util.List;
import org.netbeans.api.project.Project;
import org.sonar.wsclient.issue.Issue;
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
    }
    
}
