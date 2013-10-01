package qubexplorer;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;

/**
 *
 * @author Victor
 */
public class SonarQube {
    private String address="http://localhost:9000";

    public SonarQube(String address) {
        this.address=address;
    }

    public SonarQube() {
    }
    
    public List<Issue> getIssues(String resource, String severity) {
        SonarClient client = SonarClient.create(address);
        IssueClient issueClient = client.issueClient();
        IssueQuery query = IssueQuery.create().componentRoots(resource).pageSize(-1);
        if(!severity.equalsIgnoreCase("any")) {
            query.severities(severity.toUpperCase());
        }
        return issueClient.find(query).list();
    }

    public static String toResource(Project project) {
        FileObject pomFile = project.getProjectDirectory().getFileObject("pom.xml");
        Model model = null;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try {
            model = mavenreader.read(new InputStreamReader(pomFile.getInputStream()));
            model.setPomFile(new File(pomFile.getPath()));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return model.getGroupId()+":"+model.getArtifactId();
    }
    
}
