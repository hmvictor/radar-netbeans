package qubexplorer.ui;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.openide.util.Exceptions;
import org.sonar.wsclient.rule.Rule;
import qubexplorer.Severity;
import qubexplorer.runner.Summary;

/**
 *
 * @author Victor
 */
public class SummaryModel extends AbstractTreeTableModel {

    public SummaryModel(Summary summary) {
        super(summary);
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public Object getValueAt(Object node, int i) {
        System.out.println(node.getClass());
        if (node instanceof Severity) {
            if (i == 0) {
                return ((Severity) node).name();
            } else {
                return (getSummary()).getCount((Severity) node);
            }
        } else if (node instanceof Rule) {
           if(i == 0) {
               return ((Rule)node).name();
           }else{
               return (getSummary()).getCount((Rule) node);
           }
        } else {
            return null;
        }
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
            return Severity.values()[i];
        } else if (parent instanceof Severity) {
            Rule[] rules = getSummary().getRules((Severity) parent).toArray(new Rule[0]);
            Arrays.sort(rules, new Comparator<Rule>() {

                @Override
                public int compare(Rule t, Rule t1) {
                    int count1=getSummary().getCount(t);
                    int count2=getSummary().getCount(t1);
                    return count1 - count2;
                }
                
            });
            return rules[i];
        } else {
            return null;
        }
    }

    public Summary getSummary() {
        return (Summary) getRoot();
    }
    
    

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof Summary) {
            return Severity.values().length;
        } else if (parent instanceof Severity) {
            return (getSummary()).getRules((Severity) parent).size();
        } else {
            return 0;
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object o1) {
        if (parent instanceof Summary) {
            return Arrays.asList(Severity.values()).indexOf(o1);
        } else if (parent instanceof Severity) {
            return -1;
        } else {
            return -1;
        }
    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame();
        Summary summary = new Summary() {
            
            @Override
            public int getCount(Severity severity) {
                return 10;
            }

            @Override
            public Set<Rule> getRules(Severity severity) {
                Map<String, String> map=new HashMap<>();
                map.put("name", "A rule to rule them all");
                Rule rule=new Rule(map);
                return new HashSet<>(Arrays.asList(rule));
            }

            @Override
            public int getCount(Rule rule) {
                return 15;
            }

        };
        JXTreeTable table = new JXTreeTable(new SummaryModel(summary)){

//            @Override
//            public TableCellRenderer getCellRenderer(int row, int column) {
//                return new SummaryTreeCellRenderer();
//            }
            
        };
        frame.add(new JScrollPane(table));
        frame.setLocationRelativeTo(null);
        frame.setSize(700, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    frame.setVisible(true);
                } catch (ClassNotFoundException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InstantiationException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IllegalAccessException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (UnsupportedLookAndFeelException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        });
    }

}
