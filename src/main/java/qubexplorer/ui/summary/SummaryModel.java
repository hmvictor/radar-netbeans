package qubexplorer.ui.summary;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.sonar.wsclient.services.Rule;
import qubexplorer.Severity;
import qubexplorer.Summary;

/**
 *
 * @author Victor
 */
public class SummaryModel extends AbstractTreeTableModel {

    private boolean skipEmptySeverity = false;
    private Severity[] severities;

    public SummaryModel(Summary summary, boolean skip) {
        super(summary);
        skipEmptySeverity = skip;
        setSeverities();
    }

    public boolean isSkipEmptySeverity() {
        return skipEmptySeverity;
    }

    public void setSkipEmptySeverity(boolean skipEmptySeverity) {
        this.skipEmptySeverity = skipEmptySeverity;
        setSeverities();
    }

    private void setSeverities() {
        if (skipEmptySeverity) {
            List<Severity> tmp = new LinkedList<>();
            for (Severity s : Severity.values()) {
                if (getSummary().getCount(s) > 0) {
                    tmp.add(s);
                }
            }
            severities = tmp.toArray(new Severity[tmp.size()]);
        } else {
            severities = Severity.values();
        }
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(Object node, int i) {
        Summary summary = getSummary();
        Object value = null;
        if (node instanceof Summary) {
            if (i == 0) {
                value = "Issues";
            } else {
                value = summary.getCount();
            }
        } else if (node instanceof Severity) {
            if (i == 0) {
                value = ((Severity) node).name();
            } else {
                value = summary.getCount((Severity) node);
            }
        } else if (node instanceof Rule) {
            if (i == 0) {
                value = ((Rule) node).getDescription();
            } else {
                value = summary.getCount((Rule) node);
            }
        }
        return value;
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return "";
        } else {
            return "Count";
        }
    }

    @Override
    public Object getChild(Object parent, int i) {
        if (parent instanceof Summary) {
            return severities[i];
        } else if (parent instanceof Severity) {
            Rule[] rules = getSummary().getRules((Severity) parent).toArray(new Rule[0]);
            Arrays.sort(rules, (Rule t, Rule t1) -> {
                int count1 = getSummary().getCount(t);
                int count2 = getSummary().getCount(t1);
                return count2 - count1;
            });
            return rules[i];
        } else {
            throw new AssertionError("Unknown parent object");
        }
    }

    public Summary getSummary() {
        return (Summary) getRoot();
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof Summary) {
            return severities.length;
        } else if (parent instanceof Severity) {
            return getSummary().getRules((Severity) parent).size();
        } else {
            return 0;
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object o1) {
        if (parent instanceof Summary) {
            return Arrays.asList(severities).indexOf(o1);
        } else if (parent instanceof Severity) {
            return -1;
        } else {
            return -1;
        }
    }

}
