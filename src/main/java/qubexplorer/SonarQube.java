package qubexplorer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;

/**
 *
 * @author Victor
 */
public class SonarQube {
    private static final int PAGE_SIZE = 500;
    private String address;

    public SonarQube(String address) {
        this.address=address;
    }

    public SonarQube() {
        this("http://localhost:9000");
    }
    
    public List<Issue> getIssues(String resource, String severity) {
        return getIssues(null, resource, severity);
    }
    
    public List<Issue> getIssues(Authentication auth, String resource, String severity) {
        SonarClient client;
        if(auth == null) {
            client = SonarClient.create(address);
        }else{
            client=SonarClient.builder().url(address).login(auth.getUsername()).password(new String(auth.getPassword())).build();
        }
        IssueClient issueClient = client.issueClient();
        List<Issue> issues=new LinkedList<>();
        Issues result;
        int pageIndex=1;
        do{
            IssueQuery query = IssueQuery.create().componentRoots(resource).pageSize(PAGE_SIZE).statuses("OPEN").pageIndex(pageIndex);
            if(!severity.equalsIgnoreCase("any")) {
                query.severities(severity.toUpperCase());
            }
            result = issueClient.find(query);
            issues.addAll(result.list());
            pageIndex++;
        }while(pageIndex <= result.paging().pages());
        return issues;
    }

    public static String toResource(Project project) {
        FileObject pomFile = project.getProjectDirectory().getFileObject("pom.xml");
        Model model = null;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try {
            model = mavenreader.read(new InputStreamReader(pomFile.getInputStream()));
            model.setPomFile(new File(pomFile.getPath()));
        } catch (IOException | XmlPullParserException ex) {
            throw new RuntimeException(ex);
        }
        return model.getGroupId()+":"+model.getArtifactId();
    }
    
}
