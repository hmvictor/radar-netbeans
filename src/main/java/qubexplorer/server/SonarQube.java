package qubexplorer.server;

import qubexplorer.filter.SeverityFilter;
import qubexplorer.filter.IssueFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.sonar.wsclient.services.ServerQuery;
import qubexplorer.UserCredentials;
import qubexplorer.AuthorizationException;
import qubexplorer.IssuesContainer;
import qubexplorer.NoSuchProjectException;
import qubexplorer.PassEncoder;
import qubexplorer.RadarIssue;
import qubexplorer.ResourceKey;
import qubexplorer.Severity;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.GenericSonarQubeProjectConfiguration;
import qubexplorer.Summary;

/**
 *
 * @author Victor
 */
public class SonarQube implements IssuesContainer{
    private static final String VIOLATIONS_DENSITY_METRICS = "violations_density";
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
    
    public String getVersion(UserCredentials userCredentials) {
        Sonar sonar;
        if(userCredentials == null) {
            sonar=Sonar.create(serverUrl);
        }else{
            sonar=Sonar.create(serverUrl, userCredentials.getUsername(), PassEncoder.decodeAsString(userCredentials.getPassword()));
        }
        ServerQuery serverQuery=new ServerQuery();
        return sonar.find(serverQuery).getVersion();
    }
    
    public double getRulesCompliance(UserCredentials userCredentials, ResourceKey resourceKey) {
        try{
            if(!existsProject(userCredentials, resourceKey)) {
                throw new NoSuchProjectException(resourceKey);
            }
            Sonar sonar;
            if(userCredentials == null) {
                sonar=Sonar.create(serverUrl);
            }else{
                sonar=Sonar.create(serverUrl, userCredentials.getUsername(), PassEncoder.decodeAsString(userCredentials.getPassword()));
            }
            ResourceQuery query=new ResourceQuery(resourceKey.toString());
            query.setMetrics(VIOLATIONS_DENSITY_METRICS);
            Resource r = sonar.find(query);
            return r.getMeasure(VIOLATIONS_DENSITY_METRICS).getValue();
        } catch(ConnectionException ex) {
            if(isError401(ex)){
                throw new AuthorizationException(ex);
            }else{
                throw ex;
            }
        }
    }
    
    @Override
    public List<RadarIssue> getIssues(UserCredentials auth, ResourceKey projectKey, IssueFilter... filters) {
        if(!existsProject(auth, projectKey)) {
            throw new NoSuchProjectException(projectKey);
        }
        IssueQuery query = IssueQuery.create().componentRoots(projectKey.toString()).pageSize(PAGE_SIZE).statuses("OPEN");
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
                        if(rule == null){
                            throw new IllegalStateException("No such rule in server: "+issue.ruleKey());
                        }
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
    
    public List<ActionPlan> getActionPlans(UserCredentials userCredentials, ResourceKey resourceKey){ 
        try{
            SonarClient client;
            if(userCredentials == null) {
                client = SonarClient.create(serverUrl);
            }else{
                client=SonarClient.builder().url(serverUrl).login(userCredentials.getUsername()).password(PassEncoder.decodeAsString(userCredentials.getPassword())).build();
            }
            return client.actionPlanClient().find(resourceKey.toString());
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
            //try Rule Search API
            return new RuleSearchClient(serverUrl).getRule(userCredentials, ruleKey);
        }catch(HttpException ex){
            if(ex.getMessage().contains("Error 404")){
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
            }else if(ex.status() == UNAUTHORIZED_RESPONSE_STATUS){
                throw new AuthorizationException(ex);
            }
            throw ex;
        }
    }
    
    public List<ResourceKey> getProjectsKeys(UserCredentials userCredentials) {
        try{
            Sonar sonar;
            if(userCredentials == null) {
                sonar=Sonar.create(serverUrl);
            }else {
                sonar=Sonar.create(serverUrl, userCredentials.getUsername(), PassEncoder.decodeAsString(userCredentials.getPassword()));
            }
            List<Resource> resources = sonar.findAll(new ResourceQuery());
            List<ResourceKey> keys=new ArrayList<>(resources.size());
            for(Resource r:resources) {
                keys.add(ResourceKey.valueOf(r.getKey()));
            }
            return keys;
        }catch(ConnectionException ex) {
            if(isError401(ex)){
                throw new AuthorizationException(ex);
            }else{
                throw ex;
            }
        }
    }

    private static boolean isError401(ConnectionException ex) {
        return ex.getMessage().contains("HTTP error: 401");
    }
    
    public List<SonarQubeProjectConfiguration> getProjects(UserCredentials userCredentials) {
        try{
            Sonar sonar;
            if(userCredentials == null) {
                sonar=Sonar.create(serverUrl);
            }else {
                sonar=Sonar.create(serverUrl, userCredentials.getUsername(), PassEncoder.decodeAsString(userCredentials.getPassword()));
            }
            List<Resource> resources = sonar.findAll(new ResourceQuery());
            List<SonarQubeProjectConfiguration> projects=new ArrayList<>(resources.size());
            for(Resource r:resources) {
                projects.add(new GenericSonarQubeProjectConfiguration(r.getName(), ResourceKey.valueOf(r.getKey()), r.getVersion()));
            }
            return projects;
        }catch(ConnectionException ex) {
            if(isError401(ex)){
                throw new AuthorizationException(ex);
            }else{
                throw ex;
            }
        }
    }
    
    public boolean existsProject(UserCredentials auth, ResourceKey projectKey){
        for(ResourceKey tmp: getProjectsKeys(auth) ){
            if(tmp.equals(projectKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Summary getSummary(UserCredentials auth, ResourceKey resourceKey, IssueFilter[] filters) {
        if(!existsProject(auth, resourceKey)) {
            throw new NoSuchProjectException(resourceKey);
        }
        ServerSummary counting=new ServerSummary();
        for(Severity severity: Severity.values()) {
            IssueFilter[] tempFilters=new IssueFilter[filters.length+1];
            tempFilters[0]=new SeverityFilter(severity);
            System.arraycopy(filters, 0, tempFilters, 1, filters.length);
            List<RadarIssue> issues = getIssues(auth, resourceKey, tempFilters);
            Map<Rule, Integer> counts=new HashMap<>();
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
        return counting;
    }
    
}
