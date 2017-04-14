package qubexplorer.ui.summary;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import qubexplorer.Classifier;
import qubexplorer.ClassifierSummary;
import qubexplorer.ClassifierType;
import qubexplorer.Rule;

/**
 *
 * @author Victor
 */
public class ClassifierSummaryModel<T extends Classifier> extends AbstractTreeTableModel {

    private final ClassifierType<T> classifierType;
    private boolean skipEmptySeverity = false;
    private List<T> classifiers;

    public ClassifierSummaryModel(ClassifierType<T> classifierType, ClassifierSummary summary, boolean skip) {
        super(summary);
        this.classifierType=classifierType;
        skipEmptySeverity = skip;
        setClassifiers();
    }

    public boolean isSkipEmptySeverity() {
        return skipEmptySeverity;
    }

    public void setSkipEmptySeverity(boolean skipEmptySeverity) {
        this.skipEmptySeverity = skipEmptySeverity;
        setClassifiers();
    }

    private void setClassifiers() {
        List<T> classifierValues = classifierType.getValues();
        if (skipEmptySeverity) {
            List<T> tmp = new LinkedList<>();
            for (T classifier : classifierValues) {
                if (getSummary().getCount(classifier) > 0) {
                    tmp.add(classifier);
                }
            }
            classifiers = tmp;
        } else {
            classifiers = classifierValues;
        }
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(Object node, int i) {
        ClassifierSummary<T> summary = getSummary();
        Object value = null;
        if (node instanceof ClassifierSummary) {
            if (i == 0) {
                value = "Issues";
            } else {
                value = summary.getCount();
            }
        } else if (node instanceof Classifier) {
            if (i == 0) {
                value = ((Classifier) node).getUserDescription();
            } else {
                value = summary.getCount((T) node);
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
        if (parent instanceof ClassifierSummary) {
            return classifiers.get(i);
        } else if (parent instanceof Classifier) {
            ClassifierSummary<T> summary = getSummary();
            Rule[] rules = summary.getRules((T) parent).toArray(new Rule[0]);
            Arrays.sort(rules, (Rule t, Rule t1) -> {
                int count1 = summary.getCount(t);
                int count2 = summary.getCount(t1);
                return count2 - count1;
            });
            return rules[i];
        } else {
            throw new AssertionError("Unknown parent object");
        }
    }

    public ClassifierSummary<T> getSummary() {
        return (ClassifierSummary) getRoot();
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof ClassifierSummary) {
            return classifiers.size();
        } else if (parent instanceof Classifier) {
            return getSummary().getRules((T) parent).size();
        } else {
            return 0;
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object o1) {
        if (parent instanceof ClassifierSummary) {
            return Arrays.asList(classifiers).indexOf(o1);
        } else if (parent instanceof Classifier) {
            return -1;
        } else {
            return -1;
        }
    }

}
