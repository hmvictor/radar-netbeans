package qubexplorer.runner;

import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.rule.Rule;
import qubexplorer.Authentication;
import qubexplorer.IssuesContainer;
import qubexplorer.Severity;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Victor
 */
public class SonarRunnerResult implements IssuesContainer {

    private final File file;

    public SonarRunnerResult(File file) {
        this.file = file;
    }

    public Summary getSummary() {
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            List<Rule> rules = null;
            List<Issue> issues = null;
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("rules")) {
                    rules = readRules(reader);
                }else if (name.equals("issues")) {
                    issues = readIssues(reader, new IssueFilter[0]);
                }else{
                    reader.skipValue();
                }
            }
            reader.endObject();
            Map<String, IntWrapper> countsByRule=new HashMap<>();
            Map<String, IntWrapper> countsBySeverity=new HashMap<>();
            Map<String, Rule> mapRules=new HashMap<>();
            for (Rule rule : rules) {
                mapRules.put(rule.key(), rule);
            }
            Map<Severity, Set<Rule>> rulesBySeverity=new HashMap<>();
            for (Issue issue : issues) {
                if(countsByRule.containsKey(issue.key())) {
                    countsByRule.get(issue.key()).add(1);
                }else{
                    countsByRule.put(issue.key(), new IntWrapper(1));
                }
                if(countsBySeverity.containsKey(issue.severity())) {
                    countsBySeverity.get(issue.severity()).add(1);
                }else{
                    countsBySeverity.put(issue.severity(), new IntWrapper(1));
                }
                Severity severity = Severity.valueOf(issue.severity().toUpperCase());
                Set<Rule> set = rulesBySeverity.get(severity);
                if(set == null) {
                    set=new HashSet<>();
                    rulesBySeverity.put(severity, set);
                }
                boolean exists=false;
                for (Rule rule : set) {
                    if(rule.key().equals(issue.ruleKey())){
                        exists=true;
                        break;
                    }
                }
                if(!exists){
                    set.add(mapRules.get(issue.ruleKey()));
                }
            }
            return new Summary(countsBySeverity, countsByRule, rulesBySeverity);
        } catch (IOException | ParseException ex) {
            throw new SonarRunnerException(ex);
        }
    }

    @Override
    public List<Issue> getIssues(Authentication auth, String resource, IssueFilter... filters) {
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            List<Issue> issues = null;
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("issues")) {
                    issues = readIssues(reader, filters);
                }else{
                    reader.skipValue();
                }
            }
            reader.endObject();
            return issues;
        } catch (IOException | ParseException ex) {
            throw new SonarRunnerException(ex);
        }
    }
    
    private List<Rule> readRules(JsonReader reader) throws IOException, ParseException {
        List<Rule> rules = new LinkedList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            Rule rule = readRule(reader);
            rules.add(rule);
        }
        reader.endArray();
        return rules;
    }

    private List<Issue> readIssues(JsonReader reader, IssueFilter[] filters) throws IOException, ParseException {
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

    private Issue readIssue(JsonReader reader) throws IOException, ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-SSSS");
        SonarRunnerIssue issue = new SonarRunnerIssue();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "key":
                    issue.setKey(reader.nextString());
                    break;
                case "component":
                    issue.setComponentKey(reader.nextString());
                    break;
                case "line":
                    String line = reader.nextString();
                    if (line != null && line.trim().length() > 0) {
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
    
    private Rule readRule(JsonReader reader) throws IOException, ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-SSSS");
        reader.beginObject();
        Map<String, String> map=new HashMap();
        while (reader.hasNext()) {
            
            String name = reader.nextName();
            switch (name) {
                case "key":
                    map.put("key", reader.nextString());
                    break;
                case "rule":
                    map.put("name", reader.nextString());
                    break;
                case "name":
                    map.put("description", reader.nextString());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        Rule rule=new Rule(map);
        reader.endObject();
        return rule;
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
