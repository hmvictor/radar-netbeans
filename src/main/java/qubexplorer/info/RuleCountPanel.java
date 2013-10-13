package qubexplorer.info;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
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
import org.sonar.wsclient.services.Rule;
import qubexplorer.Severity;

/**
 *
 * @author Victor
 */
public class RuleCountPanel extends javax.swing.JPanel {

    private JButton[] buttons = new JButton[0];
    private JTextField[] fields = new JTextField[0];
    private Severity severity;
    private List<ActionListener> actionListeners = new CopyOnWriteArrayList<>();

    /**
     * Creates new form SeverityPanel
     */
    public RuleCountPanel() {
        initComponents();
        GroupLayout layout = (GroupLayout) getLayout();
        layout.setHonorsVisibility(true);
        Dimension size = layout.preferredLayoutSize(this);
        setPreferredSize(size);
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
        severityButton.setText("<html><u>"+severity.toString()+"</u></html>");
        severityButton.putClientProperty("severity", severity);
    }

    public void addActionListener(ActionListener listener) {
        actionListeners.add(listener);
    }

    public void removeItemListener(ItemListener listener) {
        actionListeners.remove(listener);
    }

    public void setRuleCounts(final Map<Rule, Integer> rulesCount) {
        removeAll();
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        NumberFormat format = NumberFormat.getIntegerInstance();
        layout.setHonorsVisibility(true);
        buttons = new JButton[rulesCount.size()];
        fields = new JTextField[rulesCount.size()];
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
        for (Rule rule : keys) {
            buttons[counter] = new JButton("<html><u>"+rule.getTitle()+"</u></html>");
            buttons[counter].putClientProperty("rule", rule);
            buttons[counter].setVisible(visible);
            buttons[counter].setBorder(null);
            buttons[counter].setContentAreaFilled(false);
            buttons[counter].setHorizontalAlignment(JButton.LEFT);
            buttons[counter].addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    severityButtonActionPerformed(ae);
                }
            });
            fields[counter] = new JTextField(format.format(rulesCount.get(rule)));
            fields[counter].setColumns(10);
            fields[counter].setOpaque(false);
            fields[counter].setEditable(false);
            fields[counter].setHorizontalAlignment(JLabel.RIGHT);
            fields[counter].setBorder(null);
            sum += rulesCount.get(rule);
            fields[counter].setVisible(visible);
            counter++;
        }
        totalCount.setText(format.format(sum));
        expandButton.setEnabled(sum > 0);
        severityButton.setEnabled(sum > 0);
        this.setLayout(layout);
        GroupLayout.ParallelGroup parallelGroup = layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(severityButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(totalCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE));
        for(int i=0; i < buttons.length; i++) {
            parallelGroup.addGroup(layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(buttons[i], javax.swing.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(fields[i], javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE));
        }
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(expandButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(parallelGroup)
                .addContainerGap())
        );
        GroupLayout.SequentialGroup sequentialGroup = layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                .addComponent(expandButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(totalCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(severityButton)))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED);
        for(int i=0; i < buttons.length; i++) {
            sequentialGroup.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(buttons[i], javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(fields[i]));
        }
        sequentialGroup.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sequentialGroup)
        );
        Dimension size = layout.preferredLayoutSize(this);
        setPreferredSize(size);
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
        severityButton = new javax.swing.JButton();
        totalCount = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jTextField1 = new javax.swing.JTextField();

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

        severityButton.setFont(severityButton.getFont().deriveFont(severityButton.getFont().getSize()+5f));
        org.openide.awt.Mnemonics.setLocalizedText(severityButton, org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.severityButton.text")); // NOI18N
        severityButton.setBorder(null);
        severityButton.setContentAreaFilled(false);
        severityButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        severityButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                severityButtonActionPerformed(evt);
            }
        });

        totalCount.setEditable(false);
        totalCount.setColumns(10);
        totalCount.setFont(totalCount.getFont().deriveFont(totalCount.getFont().getSize()+5f));
        totalCount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        totalCount.setText(org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.totalCount.text")); // NOI18N
        totalCount.setBorder(null);
        totalCount.setOpaque(false);

        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.jButton1.text")); // NOI18N
        jButton1.setContentAreaFilled(false);
        jButton1.setHideActionText(true);
        jButton1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);

        jTextField1.setEditable(false);
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextField1.setText(org.openide.util.NbBundle.getMessage(RuleCountPanel.class, "RuleCountPanel.jTextField1.text")); // NOI18N
        jTextField1.setBorder(null);
        jTextField1.setOpaque(false);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(expandButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(severityButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(totalCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(expandButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(totalCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(severityButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton1))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void expandButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_expandButtonActionPerformed
        boolean visible = evt.getActionCommand().equals("+");
        for (JButton jButton : buttons) {
            jButton.setVisible(visible);
        }
        for (JTextField field : fields) {
            field.setVisible(visible);
        }
        expandButton.setText(visible ? "-" : "+");
        LayoutManager layout = getLayout();
        Dimension size = layout.preferredLayoutSize(this);
        setPreferredSize(size);
    }//GEN-LAST:event_expandButtonActionPerformed

    private void severityButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_severityButtonActionPerformed
        for (ActionListener listener : actionListeners) {
            listener.actionPerformed(evt);
        }
    }//GEN-LAST:event_severityButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton expandButton;
    private javax.swing.JButton jButton1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JButton severityButton;
    private javax.swing.JTextField totalCount;
    // End of variables declaration//GEN-END:variables
}
