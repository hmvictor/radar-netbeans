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
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
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
    
    public double getRulesCompliance(Authentication auth, String resource) {
        Sonar sonar;
        if(auth == null) {
            sonar=Sonar.create(address);
        }else{
            sonar=Sonar.create(address, auth.getUsername(), new String(auth.getPassword()));
        }
        ResourceQuery query=new ResourceQuery(resource);
        query.setMetrics("violations_density");
        Resource r = sonar.find(query);
        return r.getMeasure("violations_density").getValue();
    }
    
    public List<IssueDecorator> getIssues(String resource, String severity) {
        return getIssues(null, resource, severity);
    }
    
    public List<IssueDecorator> getIssues(Authentication auth, String resource, String severity) {
        SonarClient client;
        if(auth == null) {
            client = SonarClient.create(address);
        }else{
            client=SonarClient.builder().url(address).login(auth.getUsername()).password(new String(auth.getPassword())).build();
        }
        IssueClient issueClient = client.issueClient();
        List<IssueDecorator> issues=new LinkedList<>();
        Map<String, Rule> rulesCache=new HashMap<>();
        Issues result;
        int pageIndex=1;
        do{
            IssueQuery query = IssueQuery.create().componentRoots(resource).pageSize(PAGE_SIZE).statuses("OPEN").pageIndex(pageIndex);
            if(!severity.equalsIgnoreCase("any")) {
                query.severities(severity.toUpperCase());
            }
            result = issueClient.find(query);
            for(Issue issue:result.list()) {
                Rule rule = rulesCache.get(issue.ruleKey());
                if(rule == null) {
                    rule=getRule(auth, issue.ruleKey());
                    rulesCache.put(issue.ruleKey(), rule);
                }
                issues.add(new IssueDecorator(issue, rule));
            }
            pageIndex++;
        }while(pageIndex <= result.paging().pages());
        return issues;
    }
    
    public List<IssueDecorator> getIssuesByRule(Authentication auth, String resource, String ruleKey) {
        SonarClient client;
        if(auth == null) {
            client = SonarClient.create(address);
        }else{
            client=SonarClient.builder().url(address).login(auth.getUsername()).password(new String(auth.getPassword())).build();
        }
        IssueClient issueClient = client.issueClient();
        List<IssueDecorator> issues=new LinkedList<>();
        Map<String, Rule> rulesCache=new HashMap<>();
        Issues result;
        int pageIndex=1;
        do{
            IssueQuery query = IssueQuery.create().componentRoots(resource).pageSize(PAGE_SIZE).statuses("OPEN").pageIndex(pageIndex).rules(ruleKey);
            result = issueClient.find(query);
            for(Issue issue:result.list()) {
                Rule rule = rulesCache.get(issue.ruleKey());
                if(rule == null) {
                    rule=getRule(auth, issue.ruleKey());
                    rulesCache.put(issue.ruleKey(), rule);
                }
                issues.add(new IssueDecorator(issue, rule));
            }
            pageIndex++;
        }while(pageIndex <= result.paging().pages());
        return issues;
    }
    
    public Rule getRule(Authentication auth, String ruleKey) {
        RuleQuery ruleQuery=new RuleQuery("java");
        String tokens[]=ruleKey.split(":");
        ruleQuery.setSearchText(tokens.length == 2? tokens[1]: ruleKey);
        Sonar sonar;
        if(auth == null) {
            sonar=Sonar.create(address);
        }else {
            sonar=Sonar.create(address, auth.getUsername(), new String(auth.getPassword()));
        }
        List<Rule> rules = sonar.findAll(ruleQuery);
        for(Rule rule:rules) {
            if(rule.getKey().equals(ruleKey)) {
                return rule;
            }
        }
        return null;
    }
    
    public Counting getCounting(String resource) {
        return getCounting(null, resource);
    }
    
    public Counting getCounting(Authentication auth, String resource) {
        Counting counting=new Counting();
        for(Severity severity: Severity.values()) {
            List<IssueDecorator> issues = getIssues(auth, resource, severity.toString());
            Map<Rule, Integer> counts=new TreeMap<>(new Comparator<Rule>(){

                @Override
                public int compare(Rule t, Rule t1) {
                    return t.getTitle().compareTo(t1.getTitle());
                }
                
            });
            for(Issue issue: issues){
                Rule rule = getRule(auth, issue.ruleKey());
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
        counting.setRulesCcompliance(getRulesCompliance(auth, resource));
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
