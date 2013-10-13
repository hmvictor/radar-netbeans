package qubexplorer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.connectors.HttpClient3Connector;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.services.Rule;
import org.sonar.wsclient.services.RuleQuery;

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
    
    public List<Issue> getIssuesByRule(Authentication auth, String resource, String rule) {
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
            IssueQuery query = IssueQuery.create().componentRoots(resource).pageSize(PAGE_SIZE).statuses("OPEN").pageIndex(pageIndex).rules(rule);
            result = issueClient.find(query);
            issues.addAll(result.list());
            pageIndex++;
        }while(pageIndex <= result.paging().pages());
        return issues;
    }
    
    public Rule getRule(String ruleKey) {
        RuleQuery ruleQuery=new RuleQuery("java");
        String tokens[]=ruleKey.split(":");
        ruleQuery.setSearchText(tokens.length == 2? tokens[1]: ruleKey);
        List<Rule> rules = new Sonar(new HttpClient3Connector(new Host(address))).findAll(ruleQuery);
        for(Rule rule:rules) {
            if(rule.getKey().equals(ruleKey)) {
                return rule;
            }
        }
        return null;
    }
    
    public Counting getCounting(String resource) {
        Counting counting=new Counting();
        for(Severity severity: Severity.values()) {
            List<Issue> issues = getIssues(resource, severity.toString());
            Map<Rule, Integer> counts=new TreeMap<>(new Comparator<Rule>(){

                @Override
                public int compare(Rule t, Rule t1) {
                    return t.getTitle().compareTo(t1.getTitle());
                }
                
            });
            for(Issue issue: issues){
                Rule rule = getRule(issue.ruleKey());
                Integer counter = counts.get(rule);
                if(counter == null) {
                    counter=1;
                }else{
                    counter=counter+1;
                }
                counts.put(rule, counter);
            }
            counting.setRuleCounts(severity, counts);
        }
        return counting;
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
