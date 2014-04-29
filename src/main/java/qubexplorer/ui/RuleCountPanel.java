package qubexplorer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.hyperlink.AbstractHyperlinkAction;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.services.Rule;
import qubexplorer.Severity;

/**
 *
 * @author Victor
 */
public class RuleCountPanel extends javax.swing.JPanel {

    private JXHyperlink[] links = new JXHyperlink[0];
    private JTextField[] fields = new JTextField[0];
    private JButton[] listButtons = new JButton[0];
    private Severity severity;
    private List<ActionListener> actionListeners = new CopyOnWriteArrayList<>();

    /**
     * Creates new form SeverityPanel
     */
    public RuleCountPanel() {
        initComponents();
        adjustSize();
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
        severityLabel.setText(capitalizeString(severity.toString()));
        listAll.putClientProperty("severity", severity);
    }

    public static String capitalizeString(String string) {
        char[] chars = string.toLowerCase().toCharArray();
        boolean found = false;
        for (int i = 0; i < chars.length; i++) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                found = true;
            } else if (Character.isWhitespace(chars[i]) || chars[i] == '.' || chars[i] == '\'') { // You can add other chars here
                found = false;
            }
        }
        return String.valueOf(chars);
    }

    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }

    private void fireActionPerformed(ActionEvent e) {
        for (ActionListener listener : actionListeners) {
            listener.actionPerformed(e);
        }
    }

    public void setRuleCounts(final Map<Rule, Integer> rulesCount) {
        panelRules.removeAll();
        NumberFormat format = NumberFormat.getIntegerInstance();
        links = new JXHyperlink[rulesCount.size()];
        fields = new JTextField[rulesCount.size()];
        listButtons = new JButton[rulesCount.size()];
        int counter = 0;
        boolean visible = expandButton.getText().equals("-");
        int sum = 0;
        List<Rule> keys = new ArrayList<>(rulesCount.keySet());
        Collections.sort(keys, Collections.reverseOrder(new Comparator<Rule>() {
            @Override
            public int compare(Rule t, Rule t1) {
                return rulesCount.get(t).compareTo(rulesCount.get(t1));
            }
        }));
        for (final Rule rule : keys) {
            links[counter] = new JXHyperlink();
            links[counter].setText(rule.getTitle());
            links[counter].setRolloverEnabled(true);
            links[counter].setForeground(Color.BLACK);
            links[counter].setUnclickedColor(Color.BLACK);
            links[counter].setClickedColor(Color.BLACK);
            links[counter].setBorder(BorderFactory.createEmptyBorder(1, expandButton.getPreferredSize().width+5, 1, 1));
            links[counter].setAction(new AbstractHyperlinkAction<Object>() {
                
                {
                    setName(rule.getTitle());
                }

                @Override
                public void actionPerformed(ActionEvent ae) {
                    RuleDialog.showRule(WindowManager.getDefault().getMainWindow(), rule);
                }
                
            });
            fields[counter] = new JTextField(format.format(rulesCount.get(rule)));
            fields[counter].setColumns(10);
            fields[counter].setOpaque(false);
            fields[counter].setEditable(false);
            fields[counter].setHorizontalAlignment(JLabel.RIGHT);
            fields[counter].setBorder(null);
            listButtons[counter] = new JButton("List Issues");
            listButtons[counter].putClientProperty("rule", rule);
            listButtons[counter].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    fireActionPerformed(ae);
                }
            });
            JPanel controls=new JPanel();
            controls.setOpaque(false);
            controls.add(fields[counter]);
            controls.add(listButtons[counter]);
            JPanel panel=new JPanel();
            panel.setLayout(new BorderLayout());
            panel.add(links[counter], BorderLayout.CENTER);
            panel.add(controls, BorderLayout.LINE_END);
            if(counter % 2 == 0) {
               panel.setBackground(new Color(230, 230, 230));
            }else{
                panel.setOpaque(false);
            }
            panelRules.add(panel);
            sum += rulesCount.get(rule);
            counter++;
        }
        totalCount.setText(format.format(sum));
        expandButton.setEnabled(sum > 0);
        expandButton.setText(sum > 0 ? "+": " ");
        listAll.setEnabled(sum > 0);
        panelRules.setVisible(visible);
        adjustSize();
        revalidate();
    }

    private void adjustSize() {
        LayoutManager layout = getLayout();
        Dimension preferredSize = layout.preferredLayoutSize(this);
        setPreferredSize(preferredSize);
        Dimension maximumSize = getMaximumSize();
        setMaximumSize(new Dimension(maximumSize.width, preferredSize.height));
    }

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        final JFrame frame = new JFrame();
        RuleCountPanel severityPanel = new RuleCountPanel();
        severityPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
        severityPanel.setSeverity(Severity.MINOR);
        Map<Rule, Integer> map = new HashMap<>();
        Rule rule1 = new Rule();
        rule1.setTitle("Class with only private constructors should be final");
        Rule rule2 = new Rule();
        rule2.setTitle("Security - Array is stored directly");
        Rule rule3 = new Rule();
        rule3.setTitle("Y");
        map.put(rule1, 10);
        map.put(rule2, 5);
        map.put(rule3, 15);
        severityPanel.setRuleCounts(map);
        frame.add(severityPanel);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setLocationRelativeTo(null);
                frame.setSize(500, 400);
                frame.setVisible(true);
            }
        });
    }
    
    public void setExpanded(boolean expanded) {
        panelRules.setVisible(expanded);
        expandButton.setText(expanded ? "-" : "+");
        adjustSize();
    }
    
    public boolean isExpanded(){
        return panelRules.isVisible();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        expandButton = new javax.swing.JButton();
        severityLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        totalCount = new javax.swing.JTextField();
        listAll = new javax.swing.JButton();
        panelRules = new javax.swing.JPanel();

        setBackground(new java.awt.Color(255, 255, 255));
        setPreferredSize(new java.awt.Dimension(533, 50));
        setLayout(new java.awt.BorderLayout());

        jPanel3.setOpaque(false);
        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel2.setOpaque(false);
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 5));

        expandButton.setFont(new java.awt.Font("Consolas", 0, 13)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(expandButton, org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.expandButton.text")); // NOI18N
        expandButton.setContentAreaFilled(false);
        expandButton.setEnabled(false);
        expandButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expandButtonActionPerformed(evt);
            }
        });
        jPanel2.add(expandButton);

        severityLabel.setFont(severityLabel.getFont().deriveFont(severityLabel.getFont().getSize()+5f));
        org.openide.awt.Mnemonics.setLocalizedText(severityLabel, org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.severityLabel.text")); // NOI18N
        jPanel2.add(severityLabel);

        jPanel3.add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanel1.setOpaque(false);

        totalCount.setEditable(false);
        totalCount.setColumns(10);
        totalCount.setFont(totalCount.getFont().deriveFont(totalCount.getFont().getSize()+5f));
        totalCount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        totalCount.setText(org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.totalCount.text")); // NOI18N
        totalCount.setBorder(null);
        totalCount.setOpaque(false);
        jPanel1.add(totalCount);

        org.openide.awt.Mnemonics.setLocalizedText(listAll, org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.listAll.text")); // NOI18N
        listAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                listAllActionPerformed(evt);
            }
        });
        jPanel1.add(listAll);

        jPanel3.add(jPanel1, java.awt.BorderLayout.LINE_END);

        add(jPanel3, java.awt.BorderLayout.PAGE_START);

        panelRules.setOpaque(false);
        panelRules.setLayout(new javax.swing.BoxLayout(panelRules, javax.swing.BoxLayout.PAGE_AXIS));
        add(panelRules, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void expandButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expandButtonActionPerformed
        setExpanded(evt.getActionCommand().equals("+"));
    }//GEN-LAST:event_expandButtonActionPerformed

    private void listAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_listAllActionPerformed
        fireActionPerformed(evt);
    }//GEN-LAST:event_listAllActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton expandButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JButton listAll;
    private javax.swing.JPanel panelRules;
    private javax.swing.JLabel severityLabel;
    private javax.swing.JTextField totalCount;
    // End of variables declaration//GEN-END:variables
}
