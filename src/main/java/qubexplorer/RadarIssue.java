package qubexplorer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import qubexplorer.ui.issues.IssueLocation;

/**
 *
 * @author Victor
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RadarIssue {

    private String key;
    @JsonProperty("component")
    private String componentKey;
    private Integer line;
    private String message;
    private String severity;
    @JsonProperty("rule")
    private String ruleKey;
    private String status;
    private Date creationDate;
    private Date updateDate;
    private Rule rule;
    private String type;

    public void setKey(String key) {
        this.key = key;
    }

    public void setComponentKey(String componentKey) {
        this.componentKey = componentKey;
    }

    public void setLine(Integer line) {
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

    public String key() {
        return key;
    }

    public String componentKey() {
        return componentKey;
    }

    public String projectKey() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public String ruleKey() {
        return ruleKey;
    }

    public String severity() {
        return severity;
    }

    public String message() {
        return message;
    }

    public Integer line() {
        return line;
    }

    public String status() {
        return status;
    }

    public String resolution() {
        return "";
    }

    public String reporter() {
        return "";
    }

    public String assignee() {
        return "";
    }

    public String author() {
        return "";
    }

    public String actionPlan() {
        return "";
    }

    public Date creationDate() {
        return creationDate;
    }

    public Date updateDate() {
        return updateDate;
    }

    public Date closeDate() {
        return null;
    }

    public String attribute(String key) {
        return "";
    }

    public Map<String, String> attributes() {
        return Collections.emptyMap();
    }

    public Long componentId() {
        return 0L;
    }

    public String debt() {
        return "";
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public Rule rule() {
        return rule;
    }

    public Severity severityObject() {
        return Severity.valueOf(severity());
    }

    public IssueLocation getLocation() {
        int lineNumber = line() == null ? 0 : line();
        return new IssueLocation(componentKey(), lineNumber);
    }
    
    public String type() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
}
