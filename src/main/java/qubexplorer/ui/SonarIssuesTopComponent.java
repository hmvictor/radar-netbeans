package qubexplorer.ui;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultRowSorter;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.services.Rule;
import qubexplorer.RadarIssue;
import qubexplorer.IssuesContainer;
import qubexplorer.Severity;
import qubexplorer.server.SonarQube;
import qubexplorer.Summary;
import qubexplorer.filter.ActionPlanFilter;
import qubexplorer.filter.IssueFilter;
import qubexplorer.filter.RuleFilter;
import qubexplorer.filter.SeverityFilter;
import qubexplorer.runner.SonarRunnerResult;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//qubexplorer.ui//Sonar//EN",
        autostore = false)
@TopComponent.Description(
        preferredID = "SonarIssuesTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "qubexplorer.ui.SonarTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_SonarAction",
        preferredID = "SonarTopComponent")
@Messages({
    "CTL_SonarAction=Sonar",
    "CTL_SonarIssuesTopComponent=SonarQube",
    "HINT_SonarIssuesTopComponent=This is a Sonar Qube Window"
})
public final class SonarIssuesTopComponent extends TopComponent {

    private IssuesContainer issuesContainer;
    private Project project;
    
    private final Comparator<Severity> severityComparator = Collections.reverseOrder(new Comparator<Severity>() {
        
        @Override
        public int compare(Severity t, Severity t1) {
            return t.compareTo(t1);
        }
        
    });

    private final Comparator<IssueLocation> locationComparator = new Comparator<IssueLocation>() {
        
        @Override
        public int compare(IssueLocation t, IssueLocation t1) {
            int result = t.getPath().compareTo(t1.getPath());
            if (result != 0) {
                return result;
            } else {
                return Integer.compare(t.getLineNumber(), t1.getLineNumber());
            }
        }
        
    };
    
    private final Action showRuleInfoAction=new AbstractAction("Show Rule Info") {
        
        @Override
        public void actionPerformed(ActionEvent ae) {
            int row = tableSummary.getSelectedRow();
            if(row != -1) {
                Object selectedNode = tableSummary.getPathForRow(row).getLastPathComponent();
                assert selectedNode instanceof Rule;
                showRuleInfo((Rule)selectedNode);
            }
        }
        
    };
    
    private final Action listIssuesAction=new AbstractAction("List Issues") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            int row=tableSummary.getSelectedRow();
            if(row != -1) {
                Object selectedNode = tableSummary.getPathForRow(row).getLastPathComponent();
                List<IssueFilter> filters=new LinkedList<>();
                if (actionPlansCombo.getSelectedItem() instanceof ActionPlan) {
                    filters.add(new ActionPlanFilter((ActionPlan) actionPlansCombo.getSelectedItem()));
                }
                if (selectedNode instanceof Severity) {
                    filters.add(new SeverityFilter((Severity) selectedNode));
                } else if (selectedNode instanceof Rule) {
                    filters.add(new RuleFilter((Rule) selectedNode));
                }
                try {
                    new IssuesWorker(issuesContainer, project, SonarQube.toResource(project), filters.toArray(new IssueFilter[0])).execute();
                } catch (IOException | XmlPullParserException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        
    };
    
    
    public Action gotoIssueAction=new AbstractAction("Go to Source") {
        
        @Override
        public void actionPerformed(ActionEvent ae) {
            IssuesTableModel model=(IssuesTableModel) issuesTable.getModel();
            int row = issuesTable.getSelectedRow();
            if (row != -1) {
                row = issuesTable.getRowSorter().convertRowIndexToModel(row);
                try {
                    IssueLocation issueLocation = model.getIssueLocation(row);
                    File file=issueLocation.getFile(project);
                    if (issueLocation.getLineNumber() <= 0) {
                        openFile(file, 1);
                    } else {
                        openFile(file, issueLocation.getLineNumber());
                    }
                } catch (IOException | XmlPullParserException ex) {
                    Exceptions.printStackTrace(ex);
                } catch(ProjectNotFoundException ex) {
                    String message = org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "ProjectNotFound", ex.getShortProjectKey());
                    DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE));
                }
            }
        }
        
    };
    
    public Action showRuleInfoForIssueAction=new AbstractAction("Show Rule Info about Issue") {
        
        @Override
        public void actionPerformed(ActionEvent ae) {
            int row=issuesTable.getSelectedRow();
            if(row != -1) {
                row = issuesTable.getRowSorter().convertRowIndexToModel(row);
                IssuesTableModel model=(IssuesTableModel) issuesTable.getModel();
                RadarIssue issue = model.getIssue(row);
                showRuleInfo(issue.rule());
            }
        }

        
    };

    public SonarIssuesTopComponent() {
        initComponents();
        issuesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        issuesTable.getColumn("").setResizable(false);
        issuesTable.getColumnModel().getColumn(0).setPreferredWidth(16);
        issuesTable.getColumnModel().getColumn(0).setMaxWidth(16);
        setName(Bundle.CTL_SonarIssuesTopComponent());
        setToolTipText(Bundle.HINT_SonarIssuesTopComponent());
        filterText.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent de) {
                filterTextChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                filterTextChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
                filterTextChanged();
            }
        });
        issuesTable.getColumnExt("").setHideable(false);
        issuesTable.getColumn("Location").setCellRenderer(new LocationRenderer());
        ((DefaultRowSorter) issuesTable.getRowSorter()).setComparator(0, severityComparator);
        ((DefaultRowSorter) issuesTable.getRowSorter()).setComparator(2, severityComparator);
        ((DefaultRowSorter) issuesTable.getRowSorter()).setComparator(3, locationComparator);
        issuesTable.getColumnExt("Severity").addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent pce) {
                if (pce.getPropertyName().equals("visible") && pce.getNewValue().equals(Boolean.TRUE)) {
                    ((DefaultRowSorter) issuesTable.getRowSorter()).setComparator(2, severityComparator);
                }
            }
        });
        showRuleInfoAction.setEnabled(false);
        listIssuesAction.setEnabled(false);
        gotoIssueAction.setEnabled(false);
        showRuleInfoForIssueAction.setEnabled(false);
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setSummary(Summary summary) {
        tableSummary.setTreeTableModel(new SummaryModel(summary));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.RIGHT);
        tableSummary.getColumn(1).setCellRenderer(renderer);
        listIssuesAction.setEnabled(false);
        showRuleInfoAction.setEnabled(false);
    }

    public void setIssuesContainer(IssuesContainer issuesContainer) {
        this.issuesContainer = issuesContainer;
        actionPlansPanel.setVisible(issuesContainer instanceof SonarQube);
    }

    public void setActionPlans(List<ActionPlan> plans) {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement(org.openide.util.NbBundle.getMessage(Bundle.class, "SonarIssuesTopComponent.actionPlansCombo.none"));
        for (ActionPlan plan : plans) {
            model.addElement(plan);
        }
        actionPlansCombo.setModel(model);
    }
    
    public void showRuleInfo(Rule rule) {
        if (issuesContainer instanceof SonarRunnerResult && rule.getDescription() == null) {
            try {
                SonarQube sonarQube = SonarQubeFactory.createForDefaultServerUrl();
                Rule ruleInServer = sonarQube.getRule(AuthenticationRepository.getInstance().getAuthentication(sonarQube.getServerUrl(), SonarQube.toResource(project)), rule.getKey());
                rule.setDescription(ruleInServer.getDescription());
            } catch (IOException | XmlPullParserException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        RuleDialog.showRule(WindowManager.getDefault().getMainWindow(), rule);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        summaryPopupMenu = new javax.swing.JPopupMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        ruleInfoMenuItem = new javax.swing.JMenuItem();
        issuesPanel = new javax.swing.JPanel();
        title = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        issuesTable = new org.jdesktop.swingx.JXTable();
        filterText = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        shownCount = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        issuesPopupMenu = new javax.swing.JPopupMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        tabbedPane = new javax.swing.JTabbedPane();
        summaryPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableSummary = new org.jdesktop.swingx.JXTreeTable();
        tableSummary.getTableHeader().setReorderingAllowed(false);
        tableSummary.setTreeCellRenderer(new SummaryTreeCellRenderer());
        tableSummary.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panelTop = new javax.swing.JPanel();
        actionPlansPanel = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        actionPlansCombo = new javax.swing.JComboBox();
        actionPlansCombo.setRenderer(new ActionPlansRenderer());
        buttonListIssues = new javax.swing.JButton();
        buttonRuleInfo = new javax.swing.JButton();

        jMenuItem1.setAction(listIssuesAction);
        org.openide.awt.Mnemonics.setLocalizedText(jMenuItem1, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jMenuItem1.text")); // NOI18N
        summaryPopupMenu.add(jMenuItem1);

        ruleInfoMenuItem.setAction(showRuleInfoAction);
        org.openide.awt.Mnemonics.setLocalizedText(ruleInfoMenuItem, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.ruleInfoMenuItem.text")); // NOI18N
        summaryPopupMenu.add(ruleInfoMenuItem);

        title.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(title, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.title.text")); // NOI18N

        issuesTable.setModel(new IssuesTableModel());
        issuesTable.setColumnControlVisible(true);
        issuesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                issuesTableMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                issuesTableMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                issuesTableMouseReleased(evt);
            }
        });
        jScrollPane2.setViewportView(issuesTable);
        issuesTable.getColumnModel().getColumn(0).setCellRenderer(new SeverityIconRenderer());

        filterText.setText(org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.filterText.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jLabel3.text")); // NOI18N

        shownCount.setEditable(false);
        shownCount.setColumns(5);
        shownCount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        shownCount.setText(org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.shownCount.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout issuesPanelLayout = new javax.swing.GroupLayout(issuesPanel);
        issuesPanel.setLayout(issuesPanelLayout);
        issuesPanelLayout.setHorizontalGroup(
            issuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(issuesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(issuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
                    .addComponent(title, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, issuesPanelLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(filterText)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(shownCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        issuesPanelLayout.setVerticalGroup(
            issuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(issuesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(title)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(issuesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(filterText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(shownCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addContainerGap())
        );

        jMenuItem2.setAction(gotoIssueAction);
        issuesPopupMenu.add(jMenuItem2);

        jMenuItem3.setAction(showRuleInfoForIssueAction);
        issuesPopupMenu.add(jMenuItem3);

        tableSummary.setRootVisible(true);
        tableSummary.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableSummaryMouseClicked(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tableSummaryMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                tableSummaryMouseReleased(evt);
            }
        });
        tableSummary.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                tableSummaryValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(tableSummary);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jLabel4.text")); // NOI18N

        actionPlansCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                actionPlansComboActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout actionPlansPanelLayout = new javax.swing.GroupLayout(actionPlansPanel);
        actionPlansPanel.setLayout(actionPlansPanelLayout);
        actionPlansPanelLayout.setHorizontalGroup(
            actionPlansPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(actionPlansPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(actionPlansCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(12, Short.MAX_VALUE))
        );
        actionPlansPanelLayout.setVerticalGroup(
            actionPlansPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(actionPlansPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(actionPlansPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(actionPlansCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        buttonListIssues.setAction(listIssuesAction);
        org.openide.awt.Mnemonics.setLocalizedText(buttonListIssues, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.buttonListIssues.text")); // NOI18N

        buttonRuleInfo.setAction(showRuleInfoAction);
        org.openide.awt.Mnemonics.setLocalizedText(buttonRuleInfo, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.buttonRuleInfo.text")); // NOI18N

        javax.swing.GroupLayout panelTopLayout = new javax.swing.GroupLayout(panelTop);
        panelTop.setLayout(panelTopLayout);
        panelTopLayout.setHorizontalGroup(
            panelTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelTopLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonListIssues)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonRuleInfo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(actionPlansPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        panelTopLayout.setVerticalGroup(
            panelTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(actionPlansPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(panelTopLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(buttonListIssues)
                    .addComponent(buttonRuleInfo))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout summaryPanelLayout = new javax.swing.GroupLayout(summaryPanel);
        summaryPanel.setLayout(summaryPanelLayout);
        summaryPanelLayout.setHorizontalGroup(
            summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(summaryPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
                    .addComponent(panelTop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        summaryPanelLayout.setVerticalGroup(
            summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(summaryPanelLayout.createSequentialGroup()
                .addComponent(panelTop, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 358, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabbedPane.addTab(org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.summaryPanel.TabConstraints.tabTitle"), summaryPanel); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void issuesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_issuesTableMouseClicked
        if(evt.isPopupTrigger()) {
            int row = issuesTable.rowAtPoint(evt.getPoint());
            if(row != -1) {
                issuesTable.changeSelection(row, row, false, false);
                issuesPopupMenu.show(issuesTable, evt.getX(), evt.getY());
            }
            return;
        }
        if (evt.getClickCount() == 2) {
            int row = issuesTable.rowAtPoint(evt.getPoint());
            if (row != -1) {
                if(issuesTable.getSelectedRow() != row){
                    issuesTable.changeSelection(row, row, false, false);
                }
                gotoIssueAction.actionPerformed(new ActionEvent(issuesTable, Event.ACTION_EVENT, "Go to Source"));
            }
        }
    }//GEN-LAST:event_issuesTableMouseClicked

    private void tableSummaryMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSummaryMouseClicked
        if(evt.isPopupTrigger()) {
            triggerPopupMenu(evt);
            return;
        }
        
        if (evt.getClickCount() != 2) {
            return;
        }
        int rowIndex = tableSummary.rowAtPoint(evt.getPoint());
        if (rowIndex < 0) {
            return;
        }
        tableSummary.changeSelection(rowIndex, rowIndex, false, false);
        listIssuesAction.actionPerformed(new ActionEvent(tableSummary, Event.ACTION_EVENT, "List Issues"));
    }//GEN-LAST:event_tableSummaryMouseClicked

    private void actionPlansComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_actionPlansComboActionPerformed
        List<IssueFilter> filters = new LinkedList<>();
        if (actionPlansCombo.getSelectedItem() instanceof ActionPlan) {
            filters.add(new ActionPlanFilter((ActionPlan) actionPlansCombo.getSelectedItem()));
        }
        try {
            new SummaryWorker(issuesContainer, project, SonarQube.toResource(project), filters.toArray(new IssueFilter[0])).execute();
        } catch (IOException | XmlPullParserException ex) {
            Exceptions.printStackTrace(ex);
        }
    }//GEN-LAST:event_actionPlansComboActionPerformed

    private void tableSummaryMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSummaryMousePressed
        if(evt.isPopupTrigger()) {
            triggerPopupMenu(evt);
        }
    }//GEN-LAST:event_tableSummaryMousePressed

    private void tableSummaryMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSummaryMouseReleased
        if(evt.isPopupTrigger()) {
            triggerPopupMenu(evt);
        }
    }//GEN-LAST:event_tableSummaryMouseReleased

    private void tableSummaryValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_tableSummaryValueChanged
        int row = tableSummary.getSelectedRow();
        listIssuesAction.setEnabled(row != -1);
        if(row != -1) {
            Object selectedNode = tableSummary.getPathForRow(row).getLastPathComponent();
            ruleInfoMenuItem.setVisible(selectedNode instanceof Rule);
            showRuleInfoAction.setEnabled(selectedNode instanceof Rule);
        }
    }//GEN-LAST:event_tableSummaryValueChanged

    private void issuesTableMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_issuesTableMousePressed
        if(evt.isPopupTrigger()) {
            int row = issuesTable.rowAtPoint(evt.getPoint());
            if(row != -1) {
                issuesTable.changeSelection(row, row, false, false);
                issuesPopupMenu.show(issuesTable, evt.getX(), evt.getY());
            }
        }
    }//GEN-LAST:event_issuesTableMousePressed

    private void issuesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_issuesTableMouseReleased
        if(evt.isPopupTrigger()) {
            int row = issuesTable.rowAtPoint(evt.getPoint());
            if(row != -1) {
                issuesTable.changeSelection(row, row, false, false);
                issuesPopupMenu.show(issuesTable, evt.getX(), evt.getY());
            }
        }
    }//GEN-LAST:event_issuesTableMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox actionPlansCombo;
    private javax.swing.JPanel actionPlansPanel;
    private javax.swing.JButton buttonListIssues;
    private javax.swing.JButton buttonRuleInfo;
    private javax.swing.JTextField filterText;
    private javax.swing.JPanel issuesPanel;
    private javax.swing.JPopupMenu issuesPopupMenu;
    private org.jdesktop.swingx.JXTable issuesTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel panelTop;
    private javax.swing.JMenuItem ruleInfoMenuItem;
    private javax.swing.JTextField shownCount;
    private javax.swing.JPanel summaryPanel;
    private javax.swing.JPopupMenu summaryPopupMenu;
    private javax.swing.JTabbedPane tabbedPane;
    private org.jdesktop.swingx.JXTreeTable tableSummary;
    private javax.swing.JLabel title;
    // End of variables declaration//GEN-END:variables

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
    }

    private void openFile(File file, int line) {
        FileObject fobj = FileUtil.toFileObject(file);
        if (fobj == null) {
            String messageTitle = org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.unexistentFile.title");
            String message = MessageFormat.format(org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.unexistentFile.text"), file.getPath());
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), message, messageTitle, JOptionPane.WARNING_MESSAGE);
            return;
        }
        DataObject dobj = null;
        try {
            dobj = DataObject.find(fobj);
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (dobj != null) {
            LineCookie lc = (LineCookie) dobj.getCookie(LineCookie.class);
            if (lc == null) {
                /* cannot do it */
                return;
            }
            Line l = lc.getLineSet().getOriginal(line - 1);
            l.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
        }
    }

    public void filterTextChanged() {
        final String text = filterText.getText().toLowerCase();
        ((DefaultRowSorter) issuesTable.getRowSorter()).setRowFilter(new RowFilter<Object, Object>() {
            @Override
            public boolean include(RowFilter.Entry<? extends Object, ? extends Object> entry) {
                for (int c = 0; c < entry.getValueCount(); c++) {
                    if (entry.getStringValue(c).toLowerCase().contains(text)) {
                        return true;
                    }
                }
                return false;
            }
        });
        showIssuesCount();
    }

    public void setIssues(IssueFilter[] filters, RadarIssue... issues) {
        IssuesTableModel model = (IssuesTableModel) issuesTable.getModel();
        model.setIssues(issues);
        StringBuilder builder = new StringBuilder();
        for (IssueFilter filter : filters) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(filter.getDescription());
        }
        if (builder.length() > 0) {
            builder.append(". ");
        }
        builder.append("Number of issues:");
        builder.append(issues.length);
        title.setText(builder.toString());
        issuesTable.getColumnExt("Rule").setVisible(true);
        issuesTable.getColumnExt("Severity").setVisible(false);
        issuesTable.getColumnExt("Project Key").setVisible(false);
        issuesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent lse) {
                int row=issuesTable.getSelectedRow();
                gotoIssueAction.setEnabled(row != -1);
                showRuleInfoForIssueAction.setEnabled(row != -1);
            }
            
        });
        showIssuesCount();
    }

    private void showIssuesCount() {
        NumberFormat format = NumberFormat.getIntegerInstance();
        shownCount.setText(format.format(issuesTable.getRowSorter().getViewRowCount()));
    }

    static  String removeBranchPart(String componentKey) {
        String[] tokens = componentKey.split(":");
        assert tokens.length >= 2;
        return tokens[0] + ":" + tokens[1];
    }

    public void showIssues(IssueFilter[] filters, RadarIssue... issues) {
        setIssues(filters, issues);
        if(tabbedPane.getTabCount() == 1){
            tabbedPane.add("Issues", issuesPanel);
        }
        tabbedPane.setSelectedIndex(1);
        gotoIssueAction.setEnabled(false);
        showRuleInfoForIssueAction.setEnabled(false);
    }

    public void showSummary(Summary summary) {
        setSummary(summary);
        if(tabbedPane.getTabCount() == 2){
            tabbedPane.removeTabAt(1);
        }
        tabbedPane.setSelectedIndex(0);
    }

    private void triggerPopupMenu(MouseEvent evt) {
        int row = tableSummary.rowAtPoint(evt.getPoint());
        if(row != -1) {
            tableSummary.changeSelection(row, row, false, false);
            summaryPopupMenu.show(tableSummary, evt.getX(), evt.getY());
        }
    }

}
