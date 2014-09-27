package qubexplorer.server;

import qubexplorer.filter.SeverityFilter;
import qubexplorer.filter.IssueFilter;
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
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.services.Rule;
import org.sonar.wsclient.services.RuleQuery;
import qubexplorer.UserCredentials;
import qubexplorer.AuthorizationException;
import qubexplorer.IssuesContainer;
import qubexplorer.NoSuchProjectException;
import qubexplorer.PassEncoder;
import qubexplorer.RadarIssue;
import qubexplorer.Severity;
import qubexplorer.Summary;

/**
 *
 * @author Victor
 */
public class SonarQube implements IssuesContainer{
    private static final int UNAUTHORIZED_RESPONSE_STATUS = 401;
    private static final int PAGE_SIZE = 500;
    private String serverUrl;

    public SonarQube(String servelUrl) {
        this.serverUrl=servelUrl;
        //remove ending '/' if needed because of a problem with the underlying http client.
        assert this.serverUrl.length() > 1;
        if(this.serverUrl.endsWith("/")) {
            this.serverUrl=this.serverUrl.substring(0, this.serverUrl.length()-1);
        }
    }

    public SonarQube() {
        this("http://localhost:9000");
    }

    public String getServerUrl() {
        return serverUrl;
    }
    
    public double getRulesCompliance(UserCredentials userCredentials, String resource) {
        try{
            if(!existsProject(userCredentials, resource)) {
                throw new NoSuchProjectException(resource);
            }
            Sonar sonar;
            if(userCredentials == null) {
                sonar=Sonar.create(serverUrl);
            }else{
                sonar=Sonar.create(serverUrl, userCredentials.getUsername(), PassEncoder.decodeAsString(userCredentials.getPassword()));
            }
            ResourceQuery query=new ResourceQuery(resource);
            query.setMetrics("violations_density");
            Resource r = sonar.find(query);
            return r.getMeasure("violations_density").getValue();
        } catch(ConnectionException ex) {
            if(ex.getMessage().contains("HTTP error: 401")){
                throw new AuthorizationException(ex);
            }else{
                throw ex;
            }
        }
    }
    
    @Override
    public List<RadarIssue> getIssues(UserCredentials auth, String projectKey, IssueFilter... filters) {
        if(!existsProject(auth, projectKey)) {
            throw new NoSuchProjectException(projectKey);
        }
        IssueQuery query = IssueQuery.create().componentRoots(projectKey).pageSize(PAGE_SIZE).statuses("OPEN");
        for(IssueFilter filter:filters) {
            filter.apply(query);
        }
        return getIssues(auth, query);
    }
    
    private List<RadarIssue> getIssues(UserCredentials userCredentials, IssueQuery query) {
        try{
            SonarClient client;
            if(userCredentials == null) {
                client = SonarClient.create(serverUrl);
            }else{
                client=SonarClient.builder().url(serverUrl).login(userCredentials.getUsername()).password(PassEncoder.decodeAsString(userCredentials.getPassword())).build();
            }
            IssueClient issueClient = client.issueClient();
            List<RadarIssue> issues=new LinkedList<>();
            Map<String, Rule> rulesCache=new HashMap<>();
            Issues result;
            int pageIndex=1;
            do{
                query.pageIndex(pageIndex);
                result = issueClient.find(query);
                for(Issue issue:result.list()) {
                    Rule rule = rulesCache.get(issue.ruleKey());
                    if(rule == null) {
                        rule=getRule(userCredentials, issue.ruleKey());
                        rulesCache.put(issue.ruleKey(), rule);
                    }
                    issues.add(new RadarIssue(issue, rule));
                }
                pageIndex++;
            }while(pageIndex <= result.paging().pages());
            return issues;
        }catch(HttpException ex) {
            if(ex.status() == UNAUTHORIZED_RESPONSE_STATUS){
                throw new AuthorizationException(ex);
            }else{
                throw ex;
            }
        }
    }
    
    public List<ActionPlan> getActionPlans(UserCredentials userCredentials, String resource){ 
        try{
            SonarClient client;
            if(userCredentials == null) {
                client = SonarClient.create(serverUrl);
            }else{
                client=SonarClient.builder().url(serverUrl).login(userCredentials.getUsername()).password(PassEncoder.decodeAsString(userCredentials.getPassword())).build();
            }
            return client.actionPlanClient().find(resource);
        }catch(HttpException ex) {
            if(ex.status() == UNAUTHORIZED_RESPONSE_STATUS){
                throw new AuthorizationException(ex);
            }else{
                throw ex;
            }
        }
    }
    
    public Rule getRule(UserCredentials userCredentials, String ruleKey) {
        try{
            RuleQuery ruleQuery=new RuleQuery("java");
            String[] tokens=ruleKey.split(":");
            ruleQuery.setSearchText(tokens.length == 2? tokens[1]: ruleKey);
            Sonar sonar;
            if(userCredentials == null) {
                sonar=Sonar.create(serverUrl);
            }else {
                sonar=Sonar.create(serverUrl, userCredentials.getUsername(), PassEncoder.decodeAsString(userCredentials.getPassword()));
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
                throw new AuthorizationException(ex);
            }else{
                throw ex;
            }
        }
    }
    
    public Counting getCounting(UserCredentials auth, String resource, IssueFilter... filters) {
        if(!existsProject(auth, resource)) {
            throw new NoSuchProjectException(resource);
        }
        Counting counting=new Counting();
        for(Severity severity: Severity.values()) {
            IssueFilter[] tempFilters=new IssueFilter[filters.length+1];
            tempFilters[0]=new SeverityFilter(severity);
            System.arraycopy(filters, 0, tempFilters, 1, filters.length);
            List<RadarIssue> issues = getIssues(auth, resource, tempFilters);
            Map<Rule, Integer> counts=new TreeMap<>(new Comparator<Rule>(){

                @Override
                public int compare(Rule t, Rule t1) {
                    return t.getTitle().compareTo(t1.getTitle());
                }
                
            });
            for(RadarIssue issue: issues){
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
    
    public List<String> getProjectsKeys(UserCredentials userCredentials) {
        try{
            Sonar sonar;
            if(userCredentials == null) {
                sonar=Sonar.create(serverUrl);
            }else {
                sonar=Sonar.create(serverUrl, userCredentials.getUsername(), PassEncoder.decodeAsString(userCredentials.getPassword()));
            }
            List<Resource> resources = sonar.findAll(new ResourceQuery());
            List<String> keys=new ArrayList<>(resources.size());
            for(Resource r:resources) {
                keys.add(r.getKey());
            }
            return keys;
        }catch(ConnectionException ex) {
            if(ex.getMessage().contains("HTTP error: 401")){
                throw new AuthorizationException(ex);
            }else{
                throw ex;
            }
        }
    }
    
    public List<SonarProject> getProjects(UserCredentials userCredentials) {
        try{
            Sonar sonar;
            if(userCredentials == null) {
                sonar=Sonar.create(serverUrl);
            }else {
                sonar=Sonar.create(serverUrl, userCredentials.getUsername(), PassEncoder.decodeAsString(userCredentials.getPassword()));
            }
            List<Resource> resources = sonar.findAll(new ResourceQuery());
            List<SonarProject> projects=new ArrayList<>(resources.size());
            for(Resource r:resources) {
                projects.add(new SonarProject(r.getKey(), r.getName()));
            }
            return projects;
        }catch(ConnectionException ex) {
            if(ex.getMessage().contains("HTTP error: 401")){
                throw new AuthorizationException(ex);
            }else{
                throw ex;
            }
        }
    }
    
    public boolean existsProject(UserCredentials auth, String projectKey){
        for(String tmp: getProjectsKeys(auth) ){
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

    @Override
    public Summary getSummary(UserCredentials authentication, String projectKey, IssueFilter[] filters) {
        return getCounting(authentication, projectKey, filters);
    }

}
