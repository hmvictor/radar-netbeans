package qubexplorer.runner;

import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.wsclient.issue.Issue;
import qubexplorer.IssuesContainer;
import qubexplorer.RadarIssue;
import qubexplorer.ResourceKey;
import qubexplorer.Rule;
import qubexplorer.Severity;
import qubexplorer.Summary;
import qubexplorer.UserCredentials;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Victor
 */
public class SonarRunnerResult implements IssuesContainer {

    private final File file;
    private List<Rule> rules;
    private Map<String, Rule> rulesByKey;

    public SonarRunnerResult(File file) {
        this.file = file;
        loadRules();
    }
    
    private void loadRules() {
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("rules".equals(name)) {
                    rules = readRules(reader);
                }else{
                    reader.skipValue();
                }
            }
            reader.endObject();
            rulesByKey=new HashMap<>();
            for (Rule rule : rules) {
                rulesByKey.put(rule.getKey(), rule);
            }
        } catch (IOException | ParseException ex) {
            throw new SonarRunnerException(ex);
        }
    }

    public SonarRunnerSummary getSummary() {
        try  {
            List<Issue> issues = readIssues();
            Map<String, IntWrapper> countsByRule=new HashMap<>();
            Map<String, IntWrapper> countsBySeverity=new HashMap<>();
            Map<Severity, Set<Rule>> rulesBySeverity=new EnumMap<>(Severity.class);
            for (Issue issue : issues) {
                if(countsByRule.containsKey(issue.ruleKey())) {
                    countsByRule.get(issue.ruleKey()).add(1);
                }else{
                    countsByRule.put(issue.ruleKey(), new IntWrapper(1));
                }
                if(countsBySeverity.containsKey(issue.severity())) {
                    countsBySeverity.get(issue.severity()).add(1);
                }else{
                    countsBySeverity.put(issue.severity(), new IntWrapper(1));
                }
                Severity severity = Severity.valueOf(issue.severity().toUpperCase());
                Set<Rule> ruleSet = rulesBySeverity.get(severity);
                if(ruleSet == null) {
                    ruleSet=new HashSet<>();
                    rulesBySeverity.put(severity, ruleSet);
                }
                /* Rule class has no equals method defined based in rule key. */
                if(!containsRule(ruleSet, issue.ruleKey())){
                    ruleSet.add(rulesByKey.get(issue.ruleKey()));
                }
            }
            return new SonarRunnerSummary(countsBySeverity, countsByRule, rulesBySeverity);
        } catch (IOException | ParseException ex) {
            throw new SonarRunnerException(ex);
        }
    }
    
    private static boolean containsRule(Set<Rule> ruleSet, String ruleKey){
        for (Rule rule : ruleSet) {
            if(ruleKey.equals(rule.getKey())){
                return true;
            }
        }
        return false;
    }
    
    private List<Issue> readIssues() throws IOException, ParseException {
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            List<Issue> issues = null;
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("issues".equals(name)) {
                    issues = readIssues(reader, new IssueFilter[0]);
                }else{
                    reader.skipValue();
                }
            }
            reader.endObject();
            return issues == null? Collections.<Issue>emptyList(): issues;
        } 
    }

    @Override
    public List<RadarIssue> getIssues(UserCredentials auth, ResourceKey resourceKey, IssueFilter... filters) {
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            List<Issue> issues = null;
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("issues".equals(name)) {
                    issues = readIssues(reader, filters);
                }else{
                    reader.skipValue();
                }
            }
            reader.endObject();
            List<RadarIssue> tmp=new ArrayList<>(issues.size());
            for (Issue issue : issues) {
                tmp.add(new RadarIssue(issue, rulesByKey.get(issue.ruleKey())));
            }
            return tmp;
        } catch (IOException | ParseException ex) {
            throw new SonarRunnerException(ex);
        }
    }
    
    private static List<Rule> readRules(JsonReader reader) throws IOException, ParseException {
        List<Rule> ruleList = new LinkedList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            Rule rule = readRule(reader);
            ruleList.add(rule);
        }
        reader.endArray();
        return ruleList;
    }

    private static List<Issue> readIssues(JsonReader reader, IssueFilter[] filters) throws IOException, ParseException {
        List<Issue> issues = new LinkedList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            Issue issue = readIssue(reader);
            boolean valid=true;
            for(IssueFilter filter:filters) {
                if(!filter.isValid(issue)){
                    valid=false;
                    break;
                }
            }
            if(valid) {
                issues.add(issue);
            }
        }
        reader.endArray();
        return issues;
    }

    private static Issue readIssue(JsonReader reader) throws IOException, ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        SonarRunnerIssue issue = new SonarRunnerIssue();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "key":
                    //remove project name at the beggining
                    issue.setKey(reader.nextString());
                    break;
                case "component":
                    issue.setComponentKey(reader.nextString());
                    break;
                case "line":
                    String line = reader.nextString();
                    if (line != null && !line.trim().isEmpty()) {
                        issue.setLine(Integer.parseInt(line));
                    }
                    break;
                case "message":
                    issue.setMessage(reader.nextString());
                    break;
                case "severity":
                    issue.setSeverity(reader.nextString());
                    break;
                case "rule":
                    issue.setRuleKey(reader.nextString());
                    break;
                case "status":
                    issue.setStatus(reader.nextString());
                    break;
                case "creationDate":
                    issue.setCreationDate(df.parse(reader.nextString()));
                    break;
                case "updateDate":
                    issue.setUpdateDate(df.parse(reader.nextString()));
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return issue;
    }
    
    private static Rule readRule(JsonReader reader) throws IOException, ParseException {
        reader.beginObject();
        Rule rule=new Rule();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "key":
                    rule.setKey(reader.nextString());
                    break;
                case "name":
                    rule.setName(reader.nextString());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        
        reader.endObject();
        return rule;
    }

    @Override
    public Summary getSummary(UserCredentials authentication, ResourceKey resourceKey, IssueFilter[] filters) {
        return getSummary();
    }

    static class IntWrapper {
        private int value;

        public IntWrapper(int val) {
            value=val;
        }
        
        public void add(int inc){
            value+=inc;
        }
        
        public int getInt(){
            return value;
        }
        
    }

}
