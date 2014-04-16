package qubexplorer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.ActionPlanQuery;
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
        //remove ending '/' if needed because of a problem with the underlying http client.
        assert this.address.length() > 1;
        if(this.address.endsWith("/")) {
            this.address=this.address.substring(0, this.address.length()-1);
        }
    }

    public SonarQube() {
        this("http://localhost:9000");
    }
    
    public double getRulesCompliance(String resource) {
        return getRulesCompliance(null, resource);
    }
    
    public double getRulesCompliance(Authentication auth, String resource) {
        try{
            if(!existsProject(auth, resource)) {
                throw new NoSuchProjectException(resource);
            }
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
        }catch(ConnectionException ex) {
            if(ex.getMessage().contains("HTTP error: 401")){
                throw new AuthorizationException();
            }else{
                throw ex;
            }
        }
    }
    
    public List<IssueDecorator> getIssuesBySeverity(String resource, Severity severity) {
        return getIssuesBySeverity(null, resource, severity);
    }
    
    public List<IssueDecorator> getIssues(Authentication auth, String resource, IssueFilter... filters) {
        if(!existsProject(auth, resource)) {
            throw new NoSuchProjectException(resource);
        }
        IssueQuery query = IssueQuery.create().componentRoots(resource).pageSize(PAGE_SIZE).statuses("OPEN");
        for(IssueFilter filter:filters) {
            filter.apply(query);
        }
        return getIssues(auth, query);
    }
    
    public List<IssueDecorator> getIssuesBySeverity(Authentication auth, String resource, Severity severity) {
        if(!existsProject(auth, resource)) {
            throw new NoSuchProjectException(resource);
        }
        IssueQuery query = IssueQuery.create().componentRoots(resource).pageSize(PAGE_SIZE).statuses("OPEN");
        if(severity != null) {
            query.severities(severity.toString().toUpperCase());
        }
        return getIssues(auth, query);
    }
    
//    public List<IssueDecorator> getIssuesByRule(String resource, String ruleKey) {
//        return getIssuesByRule(null, resource, ruleKey);
//    }
    
//    public List<IssueDecorator> getIssuesByRule(Authentication auth, String resource, String ruleKey) {
//        return getIssues(auth, resource, new RuleFilter(ruleKey));
//        if(!existsProject(auth, resource)) {
//            throw new NoSuchProjectException(resource);
//        }
//        IssueQuery query = IssueQuery.create().componentRoots(resource).pageSize(PAGE_SIZE).statuses("OPEN").rules(ruleKey);
//        return getIssues(auth, query);
//    }
    
    private List<IssueDecorator> getIssues(Authentication auth, IssueQuery query) {
        try{
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
                query.pageIndex(pageIndex);
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
        }catch(HttpException ex) {
            if(ex.status() == 401){
                throw new AuthorizationException();
            }else{
                throw ex;
            }
        }
    }
    
    public List<ActionPlan> getActionPlans(Authentication auth, String resource){ 
        SonarClient client;
        if(auth == null) {
            client = SonarClient.create(address);
        }else{
            client=SonarClient.builder().url(address).login(auth.getUsername()).password(new String(auth.getPassword())).build();
        }
        return client.actionPlanClient().find(resource);
    }
    
    public Rule getRule(String ruleKey) {
        return getRule(null, ruleKey);
    }
    
    public Rule getRule(Authentication auth, String ruleKey) {
        try{
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
        }catch(ConnectionException ex) {
            if(ex.getMessage().contains("HTTP error: 401")){
                throw new AuthorizationException();
            }else{
                throw ex;
            }
        }
    }
    
    public Counting getCounting(String resource) {
        return getCounting(null, resource);
    }
    
    public Counting getCounting(Authentication auth, String resource, IssueFilter... filters) {
        if(!existsProject(auth, resource)) {
            throw new NoSuchProjectException(resource);
        }
        Counting counting=new Counting();
        for(Severity severity: Severity.values()) {
            IssueFilter[] tempFilters=new IssueFilter[filters.length+1];
            tempFilters[0]=new SeverityFilter(severity);
            System.arraycopy(filters, 0, tempFilters, 1, filters.length);
            List<IssueDecorator> issues = getIssues(auth, resource, tempFilters);
            Map<Rule, Integer> counts=new TreeMap<>(new Comparator<Rule>(){

                @Override
                public int compare(Rule t, Rule t1) {
                    return t.getTitle().compareTo(t1.getTitle());
                }
                
            });
            for(IssueDecorator issue: issues){
                Integer counter = counts.get(issue.rule());
                if(counter == null) {
                    counter=1;
                }else{
                    counter=counter+1;
                }
                counts.put(issue.rule(), counter);
            }
            counting.setRuleCounts(severity, counts);
        }
        counting.setRulesCompliance(getRulesCompliance(auth, resource));
        return counting;
    }
    
    public long getIssuesCount(Authentication auth, String resource, Severity severity) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    public List<String> getProjects(Authentication auth) {
        try{
            Sonar sonar;
            if(auth == null) {
                sonar=Sonar.create(address);
            }else {
                sonar=Sonar.create(address, auth.getUsername(), new String(auth.getPassword()));
            }
            List<Resource> resources = sonar.findAll(new ResourceQuery());
            List<String> keys=new ArrayList<>(resources.size());
            for(Resource r:resources) {
                keys.add(r.getKey());
            }
            return keys;
        }catch(ConnectionException ex) {
            if(ex.getMessage().contains("HTTP error: 401")){
                throw new AuthorizationException();
            }else{
                throw ex;
            }
        }
    }
    
    public boolean existsProject(Authentication auth, String projectKey){
        for(String tmp: getProjects(auth) ){
            if(tmp.equals(projectKey)) {
                return true;
            }
        }
        return false;
    }

    public static String toResource(Project project) throws IOException, XmlPullParserException {
        FileObject pomFile = project.getProjectDirectory().getFileObject("pom.xml");
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        Model model = mavenreader.read(new InputStreamReader(pomFile.getInputStream()));
        model.setPomFile(new File(pomFile.getPath()));
        return model.getGroupId()+":"+model.getArtifactId();
    }
    
}
