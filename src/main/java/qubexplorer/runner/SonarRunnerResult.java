package qubexplorer.runner;

import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import org.sonar.wsclient.issue.Issue;
import qubexplorer.Authentication;
import qubexplorer.IssuesContainer;
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
        throw new UnsupportedOperationException("Not yet implemented");
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

}
