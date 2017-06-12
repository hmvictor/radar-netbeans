package qubexplorer.server;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Víctor
 */
public class Paging {
    
    private Integer pageIndex;
    private Integer pageSize;
    @JsonProperty(value = "total")
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
        return (int) Math.ceil(totalNumberOfResults / pageSize.doubleValue());
    }
    
}
