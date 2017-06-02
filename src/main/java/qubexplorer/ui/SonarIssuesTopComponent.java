package qubexplorer.ui;

import qubexplorer.server.ui.SonarQubeFactory;
import qubexplorer.ui.issues.IssuesTableModel;
import qubexplorer.ui.issues.IssuesTask;
import qubexplorer.ui.issues.LocationRenderer;
import qubexplorer.ui.issues.IssueLocation;
import qubexplorer.ui.summary.SummaryTreeCellRenderer;
import qubexplorer.ProjectNotFoundException;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultRowSorter;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import qubexplorer.Classifier;
import qubexplorer.ClassifierSummary;
import qubexplorer.RadarIssue;
import qubexplorer.IssuesContainer;
import qubexplorer.Rule;
import qubexplorer.Severity;
import qubexplorer.SummaryOptions;
import qubexplorer.server.SonarQube;
import qubexplorer.filter.AssigneesFilter;
import qubexplorer.filter.IssueFilter;
import qubexplorer.filter.RuleFilter;
import qubexplorer.filter.SeverityFilter;
import qubexplorer.runner.SonarRunnerResult;
import qubexplorer.server.SimpleClassifierSummary;
import qubexplorer.server.ui.SummarySettingsDialog;
import qubexplorer.ui.summary.ClassifierSummaryModel;
import qubexplorer.ui.summary.SummaryTask;
import qubexplorer.ui.task.TaskExecutor;

/**
 * Top component for issues.
 *
 * This component uses icons from the Silk Icon Set at
 * http://famfamfam.com/lab/icons/silk/.
 *
 */
@ConvertAsProperties(
        dtd = "-//qubexplorer.ui//Sonar//EN",
        autostore = false)
@TopComponent.Description(
        preferredID = "SonarIssuesTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "output", openAtStartup = false)
@ActionID(category = "Window", id = "qubexplorer.ui.SonarTopComponent")
@ActionReference(path = "Menu/Window")
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_SonarAction",
        preferredID = "SonarTopComponent")
@Messages({
    "CTL_SonarAction=Sonar",
    "CTL_SonarIssuesTopComponent=SonarQube",
    "HINT_SonarIssuesTopComponent=This is a Sonar Qube Window"
})
public final class SonarIssuesTopComponent extends TopComponent {

    private static final Logger LOGGER = Logger.getLogger(SonarIssuesTopComponent.class.getName());

    private transient IssuesContainer issuesContainer;
    private ProjectContext projectContext;

    private ImageIcon informationIcon = new ImageIcon(getClass().getResource("/qubexplorer/ui/images/information.png"));

    private final Comparator<Severity> severityComparator = Collections.reverseOrder(Enum::compareTo);
    
    private SummaryOptions<?> summaryOptions;

    private final AbstractAction showRuleInfoAction = new AbstractAction("Show Rule Info", informationIcon) {

        {
            putValue(Action.SHORT_DESCRIPTION, "Shows information about SonarQube rule");
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            int row = tableSummary.getSelectedRow();
            if (row != -1) {
                Object selectedNode = tableSummary.getPathForRow(row).getLastPathComponent();
                assert selectedNode instanceof Rule;
                showRuleInfo((Rule) selectedNode);
            }
        }

    };

    private final AbstractAction listIssuesAction = new AbstractAction("List Issues", new ImageIcon(getClass().getResource("/qubexplorer/ui/images/application_view_list.png"))) {

        {
            putValue(Action.SHORT_DESCRIPTION, "Displays SonarQube issues");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int row = tableSummary.getSelectedRow();
            if (row != -1) {
                listIssues(tableSummary.getPathForRow(row).getLastPathComponent());
            }
        }

    };

    private final AbstractAction gotoIssueAction = new AbstractAction("Go to Source") {

        {
            putValue(Action.SHORT_DESCRIPTION, "Opens the location of this issue in the source code");
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            IssuesTableModel model = (IssuesTableModel) issuesTable.getModel();
            int row = SonarIssuesTopComponent.this.clickedRow;
            if (row != -1) {
                openIssueLocation(model.getIssueLocation(issuesTable.getRowSorter().convertRowIndexToModel(row)));
            }
        }

    };

    private final AbstractAction showRuleInfoForIssueAction = new AbstractAction("Show Rule Info about Issue", informationIcon) {

        {
            putValue(Action.SHORT_DESCRIPTION, "Shows information about the SonarQube rule for the issue");
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            int row = SonarIssuesTopComponent.this.clickedRow;
            if (row != -1) {
                row = issuesTable.getRowSorter().convertRowIndexToModel(row);
                IssuesTableModel model = (IssuesTableModel) issuesTable.getModel();
                RadarIssue issue = model.getIssue(row);
                showRuleInfo(issue.rule());
            }
        }

    };
    
    private final AbstractAction reloadAction = new AbstractAction("Reload Summary", new ImageIcon(getClass().getResource("/qubexplorer/ui/images/arrow_refresh_small.png"))) {

        {
            putValue(Action.SHORT_DESCRIPTION, "Reload Summary");
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            SummaryOptions summaryOptions=null;
            if(issuesContainer instanceof SonarQube) {
                SummarySettingsDialog dialog=new SummarySettingsDialog(null, true);
                dialog.setClassifierType(SonarIssuesTopComponent.this.summaryOptions.getClassifierType());
                for (IssueFilter filter : SonarIssuesTopComponent.this.summaryOptions.getFilters()) {
                    if(filter instanceof AssigneesFilter) {
                        dialog.setAssignees(((AssigneesFilter)filter).getAssignees().toArray(new String[0]));
                    }
                }
                if(dialog.showDialog() == SummarySettingsDialog.Option.ACCEPT) {
                    List<IssueFilter> filters=new LinkedList<>();
                    String[] asignees = dialog.getAssignees();
                    if(asignees.length > 0) {
                        filters.add(new AssigneesFilter(asignees));
                    }
                    summaryOptions=new SummaryOptions(dialog.getClassifierType(), filters);
                }
            }
            if(summaryOptions != null) {
                TaskExecutor.execute(new SummaryTask(issuesContainer, projectContext, summaryOptions));
            }
        }

    };

    private final ItemListener skipEmptySeverities = new ItemListener() {

        @Override
        public void itemStateChanged(ItemEvent ie) {
            ClassifierSummaryModel summaryModel = (ClassifierSummaryModel) tableSummary.getTreeTableModel();
            summaryModel.setSkipEmptySeverity(!showEmptySeverity.isSelected());
            SwingUtilities.updateComponentTreeUI(tableSummary);
        }

    };

    private final IssueEditorAnnotationAttacher attacher=new IssueEditorAnnotationAttacher();
    private IssueLocation.ProjectKeyChecker projectKeyChecker;

    public SonarIssuesTopComponent() {
        initComponents();
        showEmptySeverity.addItemListener(skipEmptySeverities);
        issuesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
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
        ((DefaultRowSorter) issuesTable.getRowSorter()).setComparator(4, severityComparator);
        ((DefaultRowSorter) issuesTable.getRowSorter()).setComparator(1, new IssueLocation.IssueLocationComparator());
        issuesTable.getColumnExt("Severity").addPropertyChangeListener((PropertyChangeEvent pce) -> {
            if ("visible".equals(pce.getPropertyName()) && pce.getNewValue().equals(Boolean.TRUE)) {
                ((DefaultRowSorter) issuesTable.getRowSorter()).setComparator(4, severityComparator);
            }
        });
        showRuleInfoAction.setEnabled(false);
        listIssuesAction.setEnabled(false);
        attacher.init();
    }

    public void setProjectContext(ProjectContext projectContext) {
        this.projectContext = projectContext;
        attacher.setProjectContext(projectContext);
        setName(String.format("SonarQube - %s", ProjectUtils.getInformation(projectContext.getProject()).getDisplayName()));
    }
    
    public void setProjectKeyChecker(IssueLocation.ProjectKeyChecker projectKeyChecker) {
        this.projectKeyChecker = projectKeyChecker;
        attacher.setProjectKeyChecker(projectKeyChecker);
    }

    public <T extends Classifier> void setSummary(SummaryOptions<T> summaryOptions, ClassifierSummary<T> summary) {
        setSummaryOptions(summaryOptions);
        tableSummary.setTreeTableModel(new ClassifierSummaryModel(summaryOptions.getClassifierType(), summary, !showEmptySeverity.isSelected()));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.RIGHT);
        tableSummary.getColumn(1).setCellRenderer(renderer);
        listIssuesAction.setEnabled(false);
        showRuleInfoAction.setEnabled(false);
    }
    
    public void setIssuesContainer(IssuesContainer issuesContainer) {
        this.issuesContainer = issuesContainer;
        reloadAction.setEnabled(issuesContainer instanceof SonarQube);
    }
    
    public void setSummaryOptions(SummaryOptions<?> options) {
        this.summaryOptions=options;
        final StringBuilder builder=new StringBuilder();
        options.getFilters().forEach((IssueFilter filter) -> {
            if(builder.length() != 0) {
                builder.append("; ");
            }
            builder.append(filter.getDescription());
        });
        summaryOptionsLabel.setText(builder.toString());
    }

    public void showRuleInfo(Rule rule) {
        if (issuesContainer instanceof SonarRunnerResult && rule.getDescription() == null) {
            SonarQube sonarQube = SonarQubeFactory.createForDefaultServerUrl();
            TaskExecutor.execute(new RuleTask(sonarQube, rule, projectContext));
        } else {
            RuleDialog.showRule(WindowManager.getDefault().getMainWindow(), rule);
        }
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
        sidebar = new javax.swing.JPanel();
        buttonListIssues = new javax.swing.JButton();
        buttonRuleInfo = new javax.swing.JButton();
        showEmptySeverity = new javax.swing.JToggleButton();
        jButton2 = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableSummary = new org.jdesktop.swingx.JXTreeTable();
        tableSummary.getTableHeader().setReorderingAllowed(false);
        tableSummary.setTreeCellRenderer(new SummaryTreeCellRenderer());
        tableSummary.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        summaryOptionsLabel = new javax.swing.JLabel();

        jMenuItem1.setAction(listIssuesAction);
        org.openide.awt.Mnemonics.setLocalizedText(jMenuItem1, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jMenuItem1.text")); // NOI18N
        summaryPopupMenu.add(jMenuItem1);

        ruleInfoMenuItem.setAction(showRuleInfoAction);
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

        buttonListIssues.setAction(listIssuesAction);
        buttonListIssues.setIcon(new javax.swing.ImageIcon(getClass().getResource("/qubexplorer/ui/images/application_view_list.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(buttonListIssues, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.buttonListIssues.text")); // NOI18N
        buttonListIssues.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        buttonListIssues.setBorderPainted(false);
        buttonListIssues.setIconTextGap(0);

        buttonRuleInfo.setAction(showRuleInfoAction);
        buttonRuleInfo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/qubexplorer/ui/images/information.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(buttonRuleInfo, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.buttonRuleInfo.text")); // NOI18N
        buttonRuleInfo.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        buttonRuleInfo.setBorderPainted(false);
        buttonRuleInfo.setIconTextGap(0);

        showEmptySeverity.setIcon(new javax.swing.ImageIcon(getClass().getResource("/qubexplorer/ui/images/eye.png"))); // NOI18N
        showEmptySeverity.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(showEmptySeverity, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.showEmptySeverity.text")); // NOI18N
        showEmptySeverity.setToolTipText(org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.showEmptySeverity.toolTipText")); // NOI18N
        showEmptySeverity.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        showEmptySeverity.setBorderPainted(false);
        showEmptySeverity.setIconTextGap(0);

        jButton2.setAction(reloadAction);
        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/qubexplorer/ui/images/arrow_refresh_small.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton2, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jButton2.text")); // NOI18N
        jButton2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        javax.swing.GroupLayout sidebarLayout = new javax.swing.GroupLayout(sidebar);
        sidebar.setLayout(sidebarLayout);
        sidebarLayout.setHorizontalGroup(
            sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(showEmptySeverity, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(buttonRuleInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(buttonListIssues, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jButton2)
        );
        sidebarLayout.setVerticalGroup(
            sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidebarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonListIssues)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonRuleInfo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showEmptySeverity)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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

        org.openide.awt.Mnemonics.setLocalizedText(summaryOptionsLabel, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.summaryOptionsLabel.text")); // NOI18N

        javax.swing.GroupLayout summaryPanelLayout = new javax.swing.GroupLayout(summaryPanel);
        summaryPanel.setLayout(summaryPanelLayout);
        summaryPanelLayout.setHorizontalGroup(
            summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(summaryPanelLayout.createSequentialGroup()
                .addComponent(sidebar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1)
                .addContainerGap())
            .addComponent(summaryOptionsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 474, Short.MAX_VALUE)
        );
        summaryPanelLayout.setVerticalGroup(
            summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(summaryPanelLayout.createSequentialGroup()
                .addGroup(summaryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
                    .addComponent(sidebar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(summaryOptionsLabel))
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

    private int clickedRow=-1;
    
    private void issuesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_issuesTableMouseClicked
        clickedRow=issuesTable.rowAtPoint(evt.getPoint());
        if (evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && clickedRow != -1) {
            gotoIssueAction.actionPerformed(new ActionEvent(issuesTable, Event.ACTION_EVENT, "Go to Source"));
        }
    }//GEN-LAST:event_issuesTableMouseClicked

    private void tableSummaryMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSummaryMouseClicked
        if (evt.isPopupTrigger()) {
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
        if (listIssuesAction.isEnabled()) {
            listIssuesAction.actionPerformed(new ActionEvent(tableSummary, Event.ACTION_EVENT, "List Issues"));
        }
    }//GEN-LAST:event_tableSummaryMouseClicked

    private void tableSummaryMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSummaryMousePressed
        if (evt.isPopupTrigger()) {
            triggerPopupMenu(evt);
        }
    }//GEN-LAST:event_tableSummaryMousePressed

    private void tableSummaryMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSummaryMouseReleased
        if (evt.isPopupTrigger()) {
            triggerPopupMenu(evt);
        }
    }//GEN-LAST:event_tableSummaryMouseReleased

    private void tableSummaryValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_tableSummaryValueChanged
        int row = tableSummary.getSelectedRow();
        if (row != -1) {
            Object selectedNode = tableSummary.getPathForRow(row).getLastPathComponent();
            showRuleInfoAction.setEnabled(selectedNode instanceof Rule);
            ClassifierSummary summary = ((ClassifierSummaryModel) tableSummary.getTreeTableModel()).getSummary();
            int count;
            if (selectedNode instanceof ClassifierSummary) {
                count = summary.getCount();
            } else if (selectedNode instanceof Severity) {
                count = summary.getCount((Severity) selectedNode);
            } else if (selectedNode instanceof Rule) {
                count = summary.getCount((Rule) selectedNode);
            } else {
                count = 0;
            }
            listIssuesAction.setEnabled(count > 0);
        } else {
            listIssuesAction.setEnabled(false);
            showRuleInfoAction.setEnabled(false);
        }
    }//GEN-LAST:event_tableSummaryValueChanged

    private void issuesTableMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_issuesTableMousePressed
        clickedRow=issuesTable.rowAtPoint(evt.getPoint());
        if (evt.isPopupTrigger() && clickedRow != -1) {
            issuesPopupMenu.show(issuesTable, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_issuesTableMousePressed

    private void issuesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_issuesTableMouseReleased
        clickedRow=issuesTable.rowAtPoint(evt.getPoint());
        if (evt.isPopupTrigger() && clickedRow != -1) {
            issuesPopupMenu.show(issuesTable, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_issuesTableMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonListIssues;
    private javax.swing.JButton buttonRuleInfo;
    private javax.swing.JTextField filterText;
    private javax.swing.JPanel issuesPanel;
    private javax.swing.JPopupMenu issuesPopupMenu;
    private org.jdesktop.swingx.JXTable issuesTable;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JMenuItem ruleInfoMenuItem;
    private javax.swing.JToggleButton showEmptySeverity;
    private javax.swing.JTextField shownCount;
    private javax.swing.JPanel sidebar;
    private javax.swing.JLabel summaryOptionsLabel;
    private javax.swing.JPanel summaryPanel;
    private javax.swing.JPopupMenu summaryPopupMenu;
    private javax.swing.JTabbedPane tabbedPane;
    private org.jdesktop.swingx.JXTreeTable tableSummary;
    private javax.swing.JLabel title;
    // End of variables declaration//GEN-END:variables

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        //Do nothing, required method
    }

    private void listIssues(Object treeTableNode) {
        List<IssueFilter> filters = new LinkedList<>();
        if (treeTableNode instanceof Severity) {
            filters.add(new SeverityFilter((Severity) treeTableNode));
        } else if (treeTableNode instanceof Rule) {
            filters.add(new RuleFilter((Rule) treeTableNode));
        }
        filters.addAll(summaryOptions.getFilters());
        TaskExecutor.execute(new IssuesTask(projectContext, issuesContainer, filters));
    }

    private void openIssueLocation(IssueLocation issueLocation) {
        try {
            FileObject fileObject = issueLocation.getFileObject(projectContext, projectKeyChecker);
            if (fileObject == null) {
                notifyFileObjectNotFound(issueLocation);
            } else {
                EditorCookie editorCookie = IssueLocation.getEditorCookie(fileObject);
                if (editorCookie != null) {
                    editorCookie.openDocument();
                    editorCookie.open();
                    Line line = issueLocation.getLine(editorCookie);
                    line.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
            Exceptions.printStackTrace(ex);
        } catch (ProjectNotFoundException ex) {
            String message = org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "ProjectNotFound", ex.getShortProjectKey());
            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE));
        }
    }

    private void notifyFileObjectNotFound(IssueLocation issueLocation) {
        File file = issueLocation.getFile(projectContext, projectKeyChecker);
        String messageTitle = org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.unexistentFile.title");
        String message = MessageFormat.format(org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.unexistentFile.text"), file.getPath());
        JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), message, messageTitle, JOptionPane.WARNING_MESSAGE);
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

    public void setIssues(List<IssueFilter> filters, RadarIssue... issues) {
        attacher.detachAnnotations();
        IssuesTableModel model = (IssuesTableModel) issuesTable.getModel();

        model.setIssues(issues);
        StringBuilder builder = new StringBuilder();
        filters.forEach((filter) -> {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(filter.getDescription());
        });

        if (builder.length() > 0) {
            builder.append(". ");
        }

        builder.append(
                "Number of issues:");
        builder.append(issues.length);

        title.setText(builder.toString());
        issuesTable.getColumnExt("Rule").setVisible(true);
        issuesTable.getColumnExt("Severity").setVisible(false);
        issuesTable.getColumnExt(
                "Project Key").setVisible(false);
        issuesTable.getColumnExt("Full Path").setVisible(false);
        showIssuesCount();
        filterText.setText("");
        if(isEditorAnnotationsEnabled()) {
            attacher.attachAnnotations(issues);
        }
    }

    private void showIssuesCount() {
        NumberFormat format = NumberFormat.getIntegerInstance();
        shownCount.setText(format.format(issuesTable.getRowSorter().getViewRowCount()));
    }

    static String removeBranchPart(String componentKey) {
        String[] tokens = componentKey.split(":");
        assert tokens.length >= 2;
        return tokens[0] + ":" + tokens[1];
    }

    public void showIssues(List<IssueFilter> filters, RadarIssue... issues) {
        setIssues(filters, issues);
        if (tabbedPane.getTabCount() == 1) {
            tabbedPane.add("Issues", issuesPanel);
        }
        tabbedPane.setSelectedIndex(1);
    }

    public <T extends Classifier> void showSummary(SummaryOptions<T> summaryOptions, ClassifierSummary<T> summary) {
        setSummary(summaryOptions, summary);
        if (tabbedPane.getTabCount() == 2) {
            tabbedPane.removeTabAt(1);
        }
        tabbedPane.setSelectedIndex(0);
    }

    private void triggerPopupMenu(MouseEvent evt) {
        int row = tableSummary.rowAtPoint(evt.getPoint());
        if (row != -1) {
            tableSummary.changeSelection(row, row, false, false);
            summaryPopupMenu.show(tableSummary, evt.getX(), evt.getY());
        }
    }

    public <T extends Classifier> void resetState() {
        if(isEditorAnnotationsEnabled()) {
            attacher.detachAnnotations();
        }
        SimpleClassifierSummary emptySummary = new SimpleClassifierSummary();
        showSummary(summaryOptions, emptySummary);
    }
    
    public void refreshEditorAnnotationsStatus() {
        IssuesTableModel model = (IssuesTableModel) issuesTable.getModel();
        if(isEditorAnnotationsEnabled() && !attacher.isAttached() && model.getRowCount() > 0) {
            attacher.attachAnnotations(model.getIssues());
        }else if(!isEditorAnnotationsEnabled() && attacher.isAttached() && model.getRowCount() > 0) {
            attacher.detachAnnotations();
        }
    }
    
    public boolean isEditorAnnotationsEnabled() {
        return NbPreferences.forModule(SonarQubeOptionsPanel.class).getBoolean("editorAnnotations.enabled", true);
    }
    
}
