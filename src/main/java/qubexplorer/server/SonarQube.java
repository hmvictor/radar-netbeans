package qubexplorer.server;

import java.net.URI;
import java.net.URISyntaxException;
import qubexplorer.filter.IssueFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.netbeans.api.keyring.Keyring;
import org.openide.util.NetworkSettings;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import qubexplorer.UserCredentials;
import qubexplorer.AuthorizationException;
import qubexplorer.Classifier;
import qubexplorer.IssuesContainer;
import qubexplorer.NoSuchProjectException;
import qubexplorer.RadarIssue;
import qubexplorer.ResourceKey;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.GenericSonarQubeProjectConfiguration;
import qubexplorer.Rule;
import qubexplorer.ClassifierSummary;
import qubexplorer.ClassifierType;
import qubexplorer.PassEncoder;

/**
 *
 * @author Victor
 */
public class SonarQube implements IssuesContainer {

    private static final String VIOLATIONS_DENSITY_METRICS = "violations_density";
    private static final int UNAUTHORIZED_RESPONSE_STATUS = 401;
    private static final int PAGE_SIZE = 500;
    private String serverUrl;

    public SonarQube(String servelUrl) {
        this.serverUrl = servelUrl;
        /* remove ending '/' if needed because of a problem with the underlying http client. */
        assert this.serverUrl.length() > 1;
        if (this.serverUrl.endsWith("/")) {
            this.serverUrl = this.serverUrl.substring(0, this.serverUrl.length() - 1);
        }
    }

    public SonarQube() {
        this("http://localhost:9000");
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public Version getVersion(UserCredentials userCredentials) {
        return new Version(getServerStatus(userCredentials).getVersion());
    }
    
    public ServerStatus getServerStatus(UserCredentials userCredentials) {
        WebTarget systemStatusTarget=getSystemStatusTarget(userCredentials);
        return systemStatusTarget.request(MediaType.APPLICATION_JSON).get(ServerStatus.class);
    }

    @Override
    public List<RadarIssue> getIssues(UserCredentials auth, ResourceKey projectKey, List<IssueFilter> filters) {
        if (!existsProject(auth, projectKey)) {
            throw new NoSuchProjectException(projectKey);
        }
        Map<String, List<String>> params=new HashMap<>();
        params.put("componentKeys", Arrays.asList(projectKey.toString()));
        params.put("ps", Arrays.asList("500"));
        params.put("statuses", Arrays.asList("OPEN"));
        filters.forEach((filter) -> {
            filter.apply(params);
        });
        return getIssues(auth, params);
    }

    private List<RadarIssue> getIssues(UserCredentials userCredentials, Map<String, List<String>> params) {// IssueQuery query) {
        try {
            WebTarget issuesTarget=getIssuesTarget(userCredentials);
            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                issuesTarget=issuesTarget.queryParam(entry.getKey(), (Object[])entry.getValue().toArray(new String[0]));
            }
            IssuesSearchResult issuesSearchResult; 
            List<RadarIssue> issues = new LinkedList<>();
            Map<String, Rule> rulesCache = new HashMap<>();
            int pageIndex = 1;
            do {
//                query.pageIndex(pageIndex);
//                result = issueClient.find(query);
                issuesSearchResult = issuesTarget.queryParam("p", pageIndex).request(MediaType.APPLICATION_JSON).get(IssuesSearchResult.class);
                
                for (RadarIssue issue : issuesSearchResult.getIssues()) {
                    Rule rule = searchInCacheOrLoadFromServer(rulesCache, issue.ruleKey(), userCredentials);
                    RadarIssue radarIssue=new RadarIssue();
                    radarIssue.setComponentKey(issue.componentKey());
                    radarIssue.setCreationDate(issue.creationDate());
                    radarIssue.setKey(issue.key());
                    radarIssue.setLine(issue.line());
                    radarIssue.setMessage(issue.message());
                    radarIssue.setRule(rule);
                    radarIssue.setRuleKey(issue.ruleKey());
                    radarIssue.setSeverity(issue.severity());
                    radarIssue.setStatus(issue.status());
                    radarIssue.setUpdateDate(issue.updateDate());
                    issues.add(radarIssue);
                }
                pageIndex++;
            } while (issuesSearchResult.getPaging().getTotal() != null && pageIndex <= Math.ceil(issuesSearchResult.getPaging().getTotal()/issuesSearchResult.getPaging().getPageSize().doubleValue()));
            return issues;
        } catch (WebApplicationException ex) {
            if (isError401(ex)) {
                throw new AuthorizationException(ex);
            } else {
                throw ex;
            }
        }
    }

    private Rule searchInCacheOrLoadFromServer(Map<String, Rule> rulesCache, String ruleKey, UserCredentials userCredentials) {
        Rule rule = rulesCache.get(ruleKey);
        if (rule == null) {
            rule = getRule(userCredentials, ruleKey);
            if (rule != null) {
                rulesCache.put(ruleKey, rule);
            }
        }
        if (rule == null) {
            throw new IllegalStateException("No such rule in server: " + ruleKey);
        }
        return rule;
    }

    public Rule getRule(UserCredentials userCredentials, String ruleKey) {
        try {
            WebTarget rulesTarget = getRulesTarget(userCredentials);
            return rulesTarget.queryParam("key", ruleKey).request(MediaType.APPLICATION_JSON).get(RuleResult.class).getRule();
        } catch (WebApplicationException ex) {
            if (isError401(ex)) {
                throw new AuthorizationException(ex);
            } else {
                throw ex;
            }
        }
    }

    public List<ResourceKey> getProjectsKeys(UserCredentials userCredentials) {
        try {
            WebTarget resourcesTarget=getResourceTarget(userCredentials);
            List<Resource> resources=resourcesTarget.request(MediaType.APPLICATION_JSON).get(new GenericType<List<Resource>>() {});
            List<ResourceKey> keys = new ArrayList<>(resources.size());
            resources.forEach((r) -> {
                keys.add(ResourceKey.valueOf(r.getKey()));
            });
            return keys;
        } catch (WebApplicationException ex) {
            if (isError401(ex)) {
                throw new AuthorizationException(ex);
            } else {
                throw ex;
            }
        }
    }

    private static boolean isError401(WebApplicationException ex) {
        return ex.getResponse().getStatus() == 401;
    }

    public List<SonarQubeProjectConfiguration> getProjects(UserCredentials userCredentials) {
        try {
            WebTarget resourcesTarget=getResourceTarget(userCredentials);
            List<Resource> resources = resourcesTarget.request(MediaType.APPLICATION_JSON).get(new GenericType<List<Resource>>() {});
            List<SonarQubeProjectConfiguration> projects = new ArrayList<>(resources.size());
            for (Resource r : resources) {
                projects.add(new GenericSonarQubeProjectConfiguration(r.getName(), ResourceKey.valueOf(r.getKey()), r.getVersion()));
            }
            return projects;
        } catch (WebApplicationException ex) {
            if (isError401(ex)) {
                throw new AuthorizationException(ex);
            } else {
                throw ex;
            }
        }
    }

    public boolean existsProject(UserCredentials auth, ResourceKey projectKey) {
        for (ResourceKey tmp : getProjectsKeys(auth)) {
            if (tmp.equals(projectKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <T extends Classifier> ClassifierSummary<T> getSummary(ClassifierType<T> classifierType, UserCredentials auth, ResourceKey resourceKey, List<IssueFilter> filters) {
        if (!existsProject(auth, resourceKey)) {
            throw new NoSuchProjectException(resourceKey);
        }
        SimpleClassifierSummary<T> simpleSummary = new SimpleClassifierSummary<>();
        List<T> values=classifierType.getValues();
        for (T classifier : values) {
            List<IssueFilter> tempFilters = new LinkedList<>();
            tempFilters.add(classifier.createFilter());
            tempFilters.addAll(filters);
            List<RadarIssue> issues = getIssues(auth, resourceKey, tempFilters);
            issues.forEach((issue) -> {
                simpleSummary.increment(classifier, issue.rule(), 1);
            });
        }
        return simpleSummary;
    }

    private WebTarget getResourceTarget(UserCredentials userCredentials) {
        ResteasyClient client = getClient(userCredentials);
        return client.target(serverUrl+"/api/resources");
    }

    private WebTarget getRulesTarget(UserCredentials userCredentials) {
        ResteasyClient client = getClient(userCredentials);
        return client.target(serverUrl+"/api/rules/show");
    }
    
    private WebTarget getIssuesTarget(UserCredentials userCredentials) {
        ResteasyClient client = getClient(userCredentials);
        return client.target(serverUrl+"/api/issues/search");
    }

    private WebTarget getSystemStatusTarget(UserCredentials userCredentials) {
        ResteasyClient client = getClient(userCredentials);
        return client.target(serverUrl+"/api/system/status");
    }
    
    private ResteasyClient getClient(UserCredentials userCredentials) {
        ClientHttpEngine httpEngine=new ApacheHttpClient4Engine(createHttpClient());
        ResteasyClient client = new ResteasyClientBuilder().httpEngine(httpEngine).build();
        if(userCredentials != null) {
            client.register(new BasicAuthentication(userCredentials.getUsername(), PassEncoder.decodeAsString(userCredentials.getPassword())));
        }
        return client;
    }
    
    private HttpClient createHttpClient() {
        final ProxySettings proxySettings = getProxySettings();
        DefaultHttpClient httpClient = new DefaultHttpClient();
        if (proxySettings != null) {
            if (proxySettings.getUsername() != null) {
                httpClient.getCredentialsProvider()
                        .setCredentials(new AuthScope(proxySettings.getHost(), proxySettings.getPort()), new UsernamePasswordCredentials(proxySettings.getUsername(), proxySettings.getPassword()));
            }
            HttpHost proxy = new HttpHost(proxySettings.getHost(), proxySettings.getPort());
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            httpClient.setRoutePlanner(routePlanner);
        }
        return httpClient;
    }

    private ProxySettings getProxySettings() {
        try {
            ProxySettings settings = null;
            URI uri = new URI(serverUrl);
            String proxyHost = NetworkSettings.getProxyHost(uri);
            if (proxyHost != null) {
                int proxyPort = 8080;
                String stringProxyPort = NetworkSettings.getProxyPort(uri);
                if (stringProxyPort != null) {
                    proxyPort = Integer.parseInt(stringProxyPort);
                }
                settings = new ProxySettings(proxyHost, proxyPort);
                String authenticationUsername = NetworkSettings.getAuthenticationUsername(uri);
                if (authenticationUsername != null) {
                    settings.setUsername(authenticationUsername);
                    settings.setKeyForPassword(NetworkSettings.getKeyForAuthenticationPassword(uri));
                }
            }
            return settings;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Wrong URI " + serverUrl, ex);
        }
    }

    private static class ProxySettings {

        private final String host;
        private final int port;
        private String username;
        private String keyForPassword;

        public ProxySettings(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return new String(Keyring.read(keyForPassword));
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setKeyForPassword(String keyForPassword) {
            this.keyForPassword = keyForPassword;
        }

    }

}
