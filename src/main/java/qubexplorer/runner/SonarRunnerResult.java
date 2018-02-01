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
import qubexplorer.Classifier;
import qubexplorer.ClassifierSummary;
import qubexplorer.ClassifierType;
import qubexplorer.IssuesContainer;
import qubexplorer.RadarIssue;
import qubexplorer.ResourceKey;
import qubexplorer.Rule;
import qubexplorer.Severity;
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
        } catch (IOException ex) {
            throw new SonarRunnerException(ex);
        }
    }

    public SonarRunnerClassifierSummary<Severity> getClassifierSummaryBySeverity() {
        try  {
            List<RadarIssue> issues = readIssues();
            Map<String, IntWrapper> countsByRule=new HashMap<>();
            Map<Severity, IntWrapper> countsBySeverity=new EnumMap<>(Severity.class);
            Map<Severity, Set<Rule>> rulesBySeverity=new EnumMap<>(Severity.class);
            for (RadarIssue issue : issues) {
                if(countsByRule.containsKey(issue.ruleKey())) {
                    countsByRule.get(issue.ruleKey()).add(1);
                }else{
                    countsByRule.put(issue.ruleKey(), new IntWrapper(1));
                }
                Severity severity = Severity.valueOf(issue.severity().toUpperCase());
                if(countsBySeverity.containsKey(severity)) {
                    countsBySeverity.get(severity).add(1);
                }else{
                    countsBySeverity.put(severity, new IntWrapper(1));
                }
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
            return new SonarRunnerClassifierSummary<>(countsBySeverity, countsByRule, rulesBySeverity);
        } catch (IOException | ParseException ex) {
            throw new SonarRunnerException(ex);
        }
    }
    
    public <T extends Classifier> SonarRunnerClassifierSummary<T> getClassifierSummary(ClassifierType<T> classifierType) {
        try  {
            List<RadarIssue> issues = readIssues();
            Map<String, IntWrapper> countsByRule=new HashMap<>();
            Map<T, IntWrapper> countsByClassifier=new HashMap<>();
            Map<T, Set<Rule>> rulesBySeverity=new HashMap<>();
            for (RadarIssue issue : issues) {
                if(countsByRule.containsKey(issue.ruleKey())) {
                    countsByRule.get(issue.ruleKey()).add(1);
                }else{
                    countsByRule.put(issue.ruleKey(), new IntWrapper(1));
                }
                T classifier = classifierType.valueOf(issue);
                if(countsByClassifier.containsKey(classifier)) {
                    countsByClassifier.get(classifier).add(1);
                }else{
                    countsByClassifier.put(classifier, new IntWrapper(1));
                }
                Set<Rule> ruleSet = rulesBySeverity.get(classifier);
                if(ruleSet == null) {
                    ruleSet=new HashSet<>();
                    rulesBySeverity.put(classifier, ruleSet);
                }
                /* Rule class has no equals method defined based in rule key. */
                if(!containsRule(ruleSet, issue.ruleKey())){
                    ruleSet.add(rulesByKey.get(issue.ruleKey()));
                }
            }
            return new SonarRunnerClassifierSummary<>(countsByClassifier, countsByRule, rulesBySeverity);
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
    
    private List<RadarIssue> readIssues() throws IOException, ParseException {
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            List<RadarIssue> issues = null;
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if ("issues".equals(name)) {
                    issues = readIssues(reader, Collections.emptyList());
                }else{
                    reader.skipValue();
                }
            }
            reader.endObject();
            return issues == null? Collections.<RadarIssue>emptyList(): issues;
        } 
    }

    @Override
    public List<RadarIssue> getIssues(UserCredentials auth, ResourceKey resourceKey, List<IssueFilter> filters) {
        try (JsonReader reader = new JsonReader(new FileReader(file))) {
            List<RadarIssue> issues = null;
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
            for (RadarIssue issue : issues) {
                issue.setRule(rulesByKey.get(issue.ruleKey()));
                tmp.add(issue);
            }
            return tmp;
        } catch (IOException | ParseException ex) {
            throw new SonarRunnerException(ex);
        }
    }
    
    private static List<Rule> readRules(JsonReader reader) throws IOException {
        List<Rule> ruleList = new LinkedList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            Rule rule = readRule(reader);
            ruleList.add(rule);
        }
        reader.endArray();
        return ruleList;
    }

    private static List<RadarIssue> readIssues(JsonReader reader, List<IssueFilter> filters) throws IOException, ParseException {
        List<RadarIssue> issues = new LinkedList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            RadarIssue issue = readIssue(reader);
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

    private static RadarIssue readIssue(JsonReader reader) throws IOException, ParseException {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        RadarIssue radarIssue = new RadarIssue();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "key":
                    //remove project name at the beggining
                    radarIssue.setKey(reader.nextString());
                    break;
                case "component":
                    radarIssue.setComponentKey(reader.nextString());
                    break;
                case "line":
                    String line = reader.nextString();
                    if (line != null && !line.trim().isEmpty()) {
                        radarIssue.setLine(Integer.parseInt(line));
                    }
                    break;
                case "message":
                    radarIssue.setMessage(reader.nextString());
                    break;
                case "severity":
                    radarIssue.setSeverity(reader.nextString());
                    break;
                case "rule":
                    radarIssue.setRuleKey(reader.nextString());
                    break;
                case "status":
                    radarIssue.setStatus(reader.nextString());
                    break;
                case "creationDate":
                    radarIssue.setCreationDate(df.parse(reader.nextString()));
                    break;
                case "updateDate":
                    radarIssue.setUpdateDate(df.parse(reader.nextString()));
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return radarIssue;
    }
    
    private static Rule readRule(JsonReader reader) throws IOException {
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
    public <T extends Classifier> ClassifierSummary<T> getSummary(ClassifierType<T> classifierType, UserCredentials authentication, ResourceKey projectKey, List<IssueFilter> filters) {
        return getClassifierSummary(classifierType);
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
