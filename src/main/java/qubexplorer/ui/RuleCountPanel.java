package qubexplorer.ui;

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
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
        GroupLayout layout = (GroupLayout) getLayout();
        layout.setHonorsVisibility(true);
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
        removeAll();
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        NumberFormat format = NumberFormat.getIntegerInstance();
        layout.setHonorsVisibility(true);
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
            links[counter].setVisible(visible);
            links[counter].setRolloverEnabled(true);
            links[counter].setForeground(Color.BLACK);
            links[counter].setUnclickedColor(Color.BLACK);
            links[counter].setClickedColor(Color.BLACK);
            links[counter].setAction(new AbstractHyperlinkAction<Object>() {
                
                {
                    setName(rule.getTitle());
                }

                @Override
                public void actionPerformed(ActionEvent ae) {
                    RuleDialog.showRule(WindowManager.getDefault().getMainWindow(), rule);
                }
                
            });
//            buttons[counter].setBorder(null);
//            buttons[counter].setContentAreaFilled(false);
//            buttons[counter].setHorizontalAlignment(JButton.LEFT);
            fields[counter] = new JTextField(format.format(rulesCount.get(rule)));
            fields[counter].setColumns(10);
            fields[counter].setOpaque(false);
            fields[counter].setEditable(false);
            fields[counter].setHorizontalAlignment(JLabel.RIGHT);
            fields[counter].setBorder(null);
            fields[counter].setVisible(visible);
            listButtons[counter] = new JButton("List Issues");
            listButtons[counter].putClientProperty("rule", rule);
            listButtons[counter].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    fireActionPerformed(ae);
                }
            });
            listButtons[counter].setVisible(visible);
            sum += rulesCount.get(rule);
            counter++;
        }
        totalCount.setText(format.format(sum));
        expandButton.setEnabled(sum > 0);
        expandButton.setText(sum > 0 ? "+": " ");
        listAll.setEnabled(sum > 0);
        this.setLayout(layout);
        GroupLayout.ParallelGroup group = layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING);
        for (JButton button : links) {
            group.addComponent(button, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE);
        }
        group.addComponent(severityLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        GroupLayout.ParallelGroup group2 = layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(totalCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(listAll));
        for (int i = 0; i < links.length; i++) {
            group2.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addComponent(fields[i], javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(listButtons[i]));
        }
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(expandButton)
                .addGap(0, 0, 0)
                .addGroup(group)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(group2)
                .addContainerGap()));
        GroupLayout.SequentialGroup group3 = layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(expandButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(severityLabel))
                .addComponent(totalCount, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(layout.createSequentialGroup()
                .addComponent(listAll)
                .addGap(3, 3, 3)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
        for (int i = 0; i < links.length; i++) {
            group3.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(links[i], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fields[i])
                    .addComponent(listButtons[i]));
        }
        group3.addContainerGap(46, Short.MAX_VALUE);
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(group3));
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        expandButton = new javax.swing.JButton();
        totalCount = new javax.swing.JTextField();
        ruleLabel = new javax.swing.JButton();
        ruleCount = new javax.swing.JTextField();
        severityLabel = new javax.swing.JLabel();
        listAll = new javax.swing.JButton();
        listButton = new javax.swing.JButton();

        setBackground(new java.awt.Color(255, 255, 255));
        setPreferredSize(new java.awt.Dimension(533, 50));

        expandButton.setFont(new java.awt.Font("Consolas", 0, 13)); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(expandButton, org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.expandButton.text")); // NOI18N
        expandButton.setContentAreaFilled(false);
        expandButton.setEnabled(false);
        expandButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                expandButtonActionPerformed(evt);
            }
        });

        totalCount.setEditable(false);
        totalCount.setColumns(10);
        totalCount.setFont(totalCount.getFont().deriveFont(totalCount.getFont().getSize()+5f));
        totalCount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        totalCount.setText(org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.totalCount.text")); // NOI18N
        totalCount.setBorder(null);
        totalCount.setOpaque(false);

        org.openide.awt.Mnemonics.setLocalizedText(ruleLabel, org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.ruleLabel.text")); // NOI18N
        ruleLabel.setContentAreaFilled(false);
        ruleLabel.setHideActionText(true);
        ruleLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

        ruleCount.setEditable(false);
        ruleCount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        ruleCount.setText(org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.ruleCount.text")); // NOI18N
        ruleCount.setBorder(null);
        ruleCount.setOpaque(false);

        severityLabel.setFont(severityLabel.getFont().deriveFont(severityLabel.getFont().getSize()+5f));
        org.openide.awt.Mnemonics.setLocalizedText(severityLabel, org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.severityLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(listAll, org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.listAll.text")); // NOI18N
        listAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                listAllActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(listButton, org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.listButton.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(expandButton)
                .addGap(0, 0, 0)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(ruleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 371, Short.MAX_VALUE)
                    .addComponent(severityLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(totalCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(listAll))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(ruleCount, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(listButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(expandButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(severityLabel))
                        .addComponent(totalCount, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(listAll)
                        .addGap(3, 3, 3)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ruleCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ruleLabel)
                    .addComponent(listButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void expandButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expandButtonActionPerformed
        boolean visible = evt.getActionCommand().equals("+");
        for (JButton jButton : links) {
            jButton.setVisible(visible);
        }
        for (JTextField field : fields) {
            field.setVisible(visible);
        }
        for (JButton button : listButtons) {
            button.setVisible(visible);
        }
        expandButton.setText(visible ? "-" : "+");
        adjustSize();
    }//GEN-LAST:event_expandButtonActionPerformed

    private void listAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_listAllActionPerformed
        fireActionPerformed(evt);
    }//GEN-LAST:event_listAllActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton expandButton;
    private javax.swing.JButton listAll;
    private javax.swing.JButton listButton;
    private javax.swing.JTextField ruleCount;
    private javax.swing.JButton ruleLabel;
    private javax.swing.JLabel severityLabel;
    private javax.swing.JTextField totalCount;
    // End of variables declaration//GEN-END:variables
}
