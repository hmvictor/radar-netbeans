package qubexplorer.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    public static class Paging {
        private Integer pageIndex;
        private Integer pageSize;
        @JsonProperty("total")
        private Integer totalNumberOfResults;

        public Integer getPageIndex() {
            return pageIndex;
        }

        public void setPageIndex(Integer pageIndex) {
            this.pageIndex = pageIndex;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        public Integer getTotalNumberOfResults() {
            return totalNumberOfResults;
        }

        public void setTotalNumberOfResults(Integer totalNumberOfResults) {
            this.totalNumberOfResults = totalNumberOfResults;
        }
        
        public int getTotalPageCount() {
            return (int) Math.ceil(totalNumberOfResults/pageSize.doubleValue());
        }
        
    }
    
}
