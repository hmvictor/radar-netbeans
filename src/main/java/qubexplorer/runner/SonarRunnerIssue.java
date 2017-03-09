package qubexplorer.runner;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueComment;

/**
 *
 * @author Victor
 */
public class SonarRunnerIssue implements Issue{
    private String key;
    private String componentKey;
    private int line;
    private String message;
    private String severity;
    private String ruleKey;
    private String status;
    private Date creationDate;
    private Date updateDate;

    public void setKey(String key) {
        this.key = key;
    }

    public void setComponentKey(String componentKey) {
        this.componentKey = componentKey;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setRuleKey(String ruleKey) {
        this.ruleKey = ruleKey;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }
    
    @Override
    public String key() {
        return key;
    }

    @Override
    public String componentKey() {
        return componentKey;
    }

    @Override
    public String projectKey() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String ruleKey() {
        return ruleKey;
    }

    @Override
    public String severity() {
        return severity;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public Integer line() {
        return line;
    }

    @Override
    public String status() {
        return status;
    }

    @Override
    public String resolution() {
        return "";
    }

    @Override
    public String reporter() {
        return "";
    }

    @Override
    public String assignee() {
        return "";
    }

    @Override
    public String author() {
        return "";
    }

    @Override
    public String actionPlan() {
        return "";
    }

    @Override
    public Date creationDate() {
        return creationDate;
    }

    @Override
    public Date updateDate() {
        return updateDate;
    }

    @Override
    public Date closeDate() {
        return null;
    }

    @Override
    public String attribute(String key) {
        return "";
    }

    @Override
    public Map<String, String> attributes() {
        return Collections.emptyMap();
    }

    @Override
    public List<IssueComment> comments() {
        return Collections.emptyList();
    }

    @Override
    public Long componentId() {
        return 0L;
    }

    @Override
    public String debt() {
        return "";
    }

}
