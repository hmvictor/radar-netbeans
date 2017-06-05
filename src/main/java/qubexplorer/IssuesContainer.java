package qubexplorer;

import java.util.List;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Victor
 */
public interface IssuesContainer {
    
    List<RadarIssue> getIssues(UserCredentials auth, ResourceKey projectKey, List<IssueFilter> filters);

    <T extends Classifier> ClassifierSummary<T> getSummary(ClassifierType<T> classifierType, UserCredentials authentication, ResourceKey projectKey, List<IssueFilter> filters);
    
}
