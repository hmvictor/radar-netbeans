package qubexplorer.server;

import java.net.URI;
import java.net.URISyntaxException;
import qubexplorer.filter.SeverityFilter;
import qubexplorer.filter.IssueFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.netbeans.api.keyring.Keyring;
import org.openide.util.NetworkSettings;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.connectors.HttpClient4Connector;
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
public class SonarQube implements IssuesContainer {

    private static final String VIOLATIONS_DENSITY_METRICS = "violations_density";
    private static final int UNAUTHORIZED_RESPONSE_STATUS = 401;
    private static final int PAGE_SIZE = 500;
    private String serverUrl;

    public SonarQube(String servelUrl) {
        this.serverUrl = servelUrl;
        //remove ending '/' if needed because of a problem with the underlying http client.
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
        Sonar sonar = createSonar(userCredentials);
        ServerQuery serverQuery = new ServerQuery();
        return new Version(sonar.find(serverQuery).getVersion());
    }

    public double getRulesCompliance(UserCredentials userCredentials, ResourceKey resourceKey) {
        try {
            if (!existsProject(userCredentials, resourceKey)) {
                throw new NoSuchProjectException(resourceKey);
            }
            Sonar sonar = createSonar(userCredentials);
            ResourceQuery query = new ResourceQuery(resourceKey.toString());
            query.setMetrics(VIOLATIONS_DENSITY_METRICS);
            Resource r = sonar.find(query);
            return r.getMeasure(VIOLATIONS_DENSITY_METRICS).getValue();
        } catch (ConnectionException ex) {
            if (isError401(ex)) {
                throw new AuthorizationException(ex);
            } else {
                throw ex;
            }
        }
    }

    @Override
    public List<RadarIssue> getIssues(UserCredentials auth, ResourceKey projectKey, IssueFilter... filters) {
        if (!existsProject(auth, projectKey)) {
            throw new NoSuchProjectException(projectKey);
        }
        IssueQuery query = IssueQuery.create().componentRoots(projectKey.toString()).pageSize(PAGE_SIZE).statuses("OPEN");
        for (IssueFilter filter : filters) {
            filter.apply(query);
        }
        return getIssues(auth, query);
    }

    private List<RadarIssue> getIssues(UserCredentials userCredentials, IssueQuery query) {
        try {
            SonarClient sonarClient = createSonarClient(userCredentials);
            IssueClient issueClient = sonarClient.issueClient();
            List<RadarIssue> issues = new LinkedList<>();
            Map<String, Rule> rulesCache = new HashMap<>();
            Issues result;
            int pageIndex = 1;
            do {
                query.pageIndex(pageIndex);
                result = issueClient.find(query);
                for (Issue issue : result.list()) {
                    Rule rule = searchInCacheOrLoadFromServer(rulesCache, issue.ruleKey(), userCredentials);
                    if (rule == null) {
                        throw new IllegalStateException("No such rule in server: " + issue.ruleKey());
                    }
                    issues.add(new RadarIssue(issue, rule));
                }
                pageIndex++;
            }while(result.paging().pages() != null && pageIndex <= result.paging().pages());
            return issues;
        } catch (HttpException ex) {
            if (ex.status() == UNAUTHORIZED_RESPONSE_STATUS) {
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
        return rule;
    }

    private Sonar createSonar(UserCredentials userCredentials) {
        Host host = new Host(serverUrl);
        if (userCredentials != null) {
            host.setUsername(userCredentials.getUsername());
            host.setPassword(PassEncoder.decodeAsString(userCredentials.getPassword()));
        }
        HttpClient4Connector connector = new HttpClient4Connector(host);
        final ProxySettings proxySettings = getProxySettings();
        if (proxySettings != null) {
            DefaultHttpClient httpClient = connector.getHttpClient();
            if (proxySettings.getUsername() != null) {
                httpClient.getCredentialsProvider()
                        .setCredentials(new AuthScope(proxySettings.getHost(), proxySettings.getPort()), new UsernamePasswordCredentials(proxySettings.getUsername(), proxySettings.getPassword()));
            }
            HttpHost proxy = new HttpHost(proxySettings.getHost(), proxySettings.getPort());
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            httpClient.setRoutePlanner(routePlanner);
        }
        return new Sonar(connector);
    }

    private SonarClient createSonarClient(UserCredentials userCredentials) {
        SonarClient.Builder builder = SonarClient.builder().url(serverUrl);
        if (userCredentials != null) {
            builder.login(userCredentials.getUsername()).password(PassEncoder.decodeAsString(userCredentials.getPassword()));
        }
        ProxySettings proxySettings = getProxySettings();
        if (proxySettings != null) {
            builder.proxy(proxySettings.getHost(), proxySettings.getPort());
            if (proxySettings.getUsername() != null) {
                builder.proxyLogin(proxySettings.getUsername()).proxyPassword(proxySettings.getPassword());
            }
        }
        return builder.build();
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

    public List<ActionPlan> getActionPlans(UserCredentials userCredentials, ResourceKey resourceKey) {
        try {
            SonarClient client = createSonarClient(userCredentials);
            return client.actionPlanClient().find(resourceKey.toString());
        } catch (HttpException ex) {
            if (ex.status() == UNAUTHORIZED_RESPONSE_STATUS) {
                throw new AuthorizationException(ex);
            } else {
                throw ex;
            }
        }
    }

    public Rule getRule(UserCredentials userCredentials, String ruleKey) {
        try {
            //try first the newest Rule Search API
            return new RuleSearchClient(serverUrl).getRule(userCredentials, ruleKey);
        } catch (HttpException ex) {
            if (ex.getMessage().contains("Error 404")) {
                //fallback to old method
                return getRuleWithQueryAPI(userCredentials, ruleKey);
            } else if (ex.status() == UNAUTHORIZED_RESPONSE_STATUS) {
                throw new AuthorizationException(ex);
            }
            throw ex;
        }
    }

    private Rule getRuleWithQueryAPI(UserCredentials userCredentials, String ruleKey) {
        RuleQuery ruleQuery = new RuleQuery("java");
        String[] tokens = ruleKey.split(":");
        ruleQuery.setSearchText(tokens.length == 2 ? tokens[1] : ruleKey);
        Sonar sonar = createSonar(userCredentials);
        List<Rule> rules = sonar.findAll(ruleQuery);
        for (Rule rule : rules) {
            if (rule.getKey().equals(ruleKey)) {
                return rule;
            }
        }
        return null;
    }

    public List<ResourceKey> getProjectsKeys(UserCredentials userCredentials) {
        try {
            Sonar sonar = createSonar(userCredentials);
            List<Resource> resources = sonar.findAll(new ResourceQuery());
            List<ResourceKey> keys = new ArrayList<>(resources.size());
            for (Resource r : resources) {
                keys.add(ResourceKey.valueOf(r.getKey()));
            }
            return keys;
        } catch (ConnectionException ex) {
            if (isError401(ex)) {
                throw new AuthorizationException(ex);
            } else {
                throw ex;
            }
        }
    }

    private static boolean isError401(ConnectionException ex) {
        return ex.getMessage().contains("HTTP error: 401");
    }

    public List<SonarQubeProjectConfiguration> getProjects(UserCredentials userCredentials) {
        try {
            Sonar sonar = createSonar(userCredentials);
            List<Resource> resources = sonar.findAll(new ResourceQuery());
            List<SonarQubeProjectConfiguration> projects = new ArrayList<>(resources.size());
            for (Resource r : resources) {
                projects.add(new GenericSonarQubeProjectConfiguration(r.getName(), ResourceKey.valueOf(r.getKey()), r.getVersion()));
            }
            return projects;
        } catch (ConnectionException ex) {
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
    public Summary getSummary(UserCredentials auth, ResourceKey resourceKey, IssueFilter[] filters) {
        if (!existsProject(auth, resourceKey)) {
            throw new NoSuchProjectException(resourceKey);
        }
        ServerSummary counting = new ServerSummary();
        for (Severity severity : Severity.values()) {
            IssueFilter[] tempFilters = new IssueFilter[filters.length + 1];
            tempFilters[0] = new SeverityFilter(severity);
            System.arraycopy(filters, 0, tempFilters, 1, filters.length);
            List<RadarIssue> issues = getIssues(auth, resourceKey, tempFilters);
            Map<Rule, Integer> counts = new HashMap<>();
            for (RadarIssue issue : issues) {
                Integer counter = counts.get(issue.rule());
                if (counter == null) {
                    counter = 1;
                } else {
                    counter = counter + 1;
                }
                counts.put(issue.rule(), counter);
            }
            counting.setRuleCounts(severity, counts);
        }
        return counting;
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
