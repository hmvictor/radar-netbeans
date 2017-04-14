package qubexplorer;

import java.util.List;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Víctor
 */
public class SummaryOptions<T extends Classifier> {
    private ClassifierType<T> classifierType;
    private List<IssueFilter> filters;

    public SummaryOptions(ClassifierType<T> classifierType, List<IssueFilter> filters) {
        this.classifierType = classifierType;
        this.filters = filters;
    }
    
    public ClassifierType<T> getClassifierType() {
        return classifierType;
    }

    public void setClassifierType(ClassifierType<T> classifierType) {
        this.classifierType = classifierType;
    }

    public List<IssueFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<IssueFilter> filters) {
        this.filters = filters;
    }
    
}
