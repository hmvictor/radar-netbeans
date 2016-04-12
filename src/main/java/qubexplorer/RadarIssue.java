package qubexplorer;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.issue.IssueComment;
import org.sonar.wsclient.services.Rule;
import qubexplorer.ui.issues.IssueLocation;

/**
 *
 * @author Victor
 */
public class RadarIssue implements Issue{
    private final Issue issue;
    private final Rule rule;

    public RadarIssue(Issue issue, Rule rule) {
        Objects.requireNonNull(issue, "issue is null");
        Objects.requireNonNull(rule, "rule is null");
        this.issue = issue;
        this.rule=rule;
    }

    @Override
    public String key() {
        return issue.key();
    }

    @Override
    public String componentKey() {
        return issue.componentKey();
    }

    @Override
    public String projectKey() {
        return issue.projectKey();
    }

    @Override
    public String ruleKey() {
        return issue.ruleKey();
    }
    
    public Rule rule() {
        return rule;
    }

    @Override
    public String severity() {
        return issue.severity();
    }
    
    public Severity severityObject(){
        return Severity.valueOf(severity());
    }

    @Override
    public String message() {
        return issue.message();
    }

    @Override
    public Integer line() {
        return issue.line();
    }

    @Override
    public Double effortToFix() {
        return issue.effortToFix();
    }

    @Override
    public String status() {
        return issue.status();
    }

    @Override
    public String resolution() {
        return issue.resolution();
    }

    @Override
    public String reporter() {
        return issue.reporter();
    }

    @Override
    public String assignee() {
        return issue.assignee();
    }

    @Override
    public String author() {
        return issue.author();
    }

    @Override
    public String actionPlan() {
        return issue.actionPlan();
    }

    @Override
    public Date creationDate() {
        return issue.creationDate();
    }

    @Override
    public Date updateDate() {
        return issue.updateDate();
    }

    @Override
    public Date closeDate() {
        return issue.closeDate();
    }

    @Override
    public String attribute(String key) {
        return issue.attribute(key);
    }

    @Override
    public Map<String, String> attributes() {
        return issue.attributes();
    }

    @Override
    public List<IssueComment> comments() {
        return issue.comments();
    }

    public IssueLocation getLocation() {
        int lineNumber=issue.line() == null ? 0: issue.line();
        return new IssueLocation(issue.componentKey(), lineNumber);
    }

    @Override
    public Long componentId() {
        return issue.componentId();
    }

    @Override
    public String debt() {
        return issue.debt();
    }

}
