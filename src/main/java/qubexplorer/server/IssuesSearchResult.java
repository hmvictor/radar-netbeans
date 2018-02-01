package qubexplorer.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import qubexplorer.RadarIssue;

/**
 *
 * @author Víctor
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssuesSearchResult {
    private Paging paging;
    private List<RadarIssue> issues;

    public Paging getPaging() {
        return paging;
    }

    public void setPaging(Paging paging) {
        this.paging = paging;
    }

    public List<RadarIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<RadarIssue> issues) {
        this.issues = issues;
    }
    
    
}
