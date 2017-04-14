package qubexplorer.filter;

import java.util.List;
import java.util.Map;
import qubexplorer.RadarIssue;

/**
 *
 * @author Victor
 */
public interface IssueFilter {

    void apply(Map<String, List<String>> params);

    String getDescription();

    boolean isValid(RadarIssue issue);

}
