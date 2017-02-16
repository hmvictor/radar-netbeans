package qubexplorer.ui;

import qubexplorer.ui.issues.FileObjectOpenedListener;
import qubexplorer.ui.issues.IssuesTableModel;
import qubexplorer.ui.issues.IssuesTask;
import qubexplorer.ui.issues.LocationRenderer;
import qubexplorer.ui.issues.IssueLocation;
import qubexplorer.ui.summary.SummaryTreeCellRenderer;
import qubexplorer.ui.summary.SummaryTask;
import qubexplorer.ui.summary.SummaryModel;
import qubexplorer.ProjectNotFoundException;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.DefaultRowSorter;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.DropDownButtonFactory;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Annotation;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
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
import qubexplorer.server.SimpleSummary;
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
        //iconBase="SET/PATH/TO/ICON/HERE", 
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

    private static final String ACTION_PLAN_PROPERTY = "actionPlan";
    private static final Logger LOGGER = Logger.getLogger(SonarIssuesTopComponent.class.getName());

    private transient IssuesContainer issuesContainer;
    private ProjectContext projectContext;
    private JPopupMenu dropDownMenu = new JPopupMenu();

    private ImageIcon informationIcon = new ImageIcon(getClass().getResource("/qubexplorer/ui/images/information.png"));

    private final Comparator<Severity> severityComparator = Collections.reverseOrder(Enum::compareTo);

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
            int row = issuesTable.getSelectedRow();
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
            int row = issuesTable.getSelectedRow();
            if (row != -1) {
                row = issuesTable.getRowSorter().convertRowIndexToModel(row);
                IssuesTableModel model = (IssuesTableModel) issuesTable.getModel();
                RadarIssue issue = model.getIssue(row);
                showRuleInfo(issue.rule());
            }
        }

    };

    private final ItemListener skipEmptySeverities = new ItemListener() {

        @Override
        public void itemStateChanged(ItemEvent ie) {
            SummaryModel summaryModel = (SummaryModel) tableSummary.getTreeTableModel();
            summaryModel.setSkipEmptySeverity(!showEmptySeverity.isSelected());
            SwingUtilities.updateComponentTreeUI(tableSummary);
        }

    };

    private final ActionListener actionPlanItemListener = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent ae) {
            List<IssueFilter> filters = new LinkedList<>();
            if (getSelectedActionPlan() != null) {
                filters.add(new ActionPlanFilter(getSelectedActionPlan()));
            }
            TaskExecutor.execute(new SummaryTask(issuesContainer, projectContext, filters.toArray(new IssueFilter[0])));
        }

    };

    private final List<Annotation> attachedAnnotations = new CopyOnWriteArrayList<>();
    
    private FileOpenedNotifier fileOpenedNotifier=new FileOpenedNotifier();
    private IssueLocation.ProjectKeyChecker projectKeyChecker;

    public SonarIssuesTopComponent() {
        initComponents();
        dropDownMenu.setToolTipText("Action Plans");
        showEmptySeverity.addItemListener(skipEmptySeverities);
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
        ((DefaultRowSorter) issuesTable.getRowSorter()).setComparator(4, severityComparator);
        ((DefaultRowSorter) issuesTable.getRowSorter()).setComparator(1, new IssueLocation.IssueLocationComparator());
        issuesTable.getColumnExt("Severity").addPropertyChangeListener((PropertyChangeEvent pce) -> {
            if ("visible".equals(pce.getPropertyName()) && pce.getNewValue().equals(Boolean.TRUE)) {
                ((DefaultRowSorter) issuesTable.getRowSorter()).setComparator(4, severityComparator);
            }
        });
        showRuleInfoAction.setEnabled(false);
        listIssuesAction.setEnabled(false);
        gotoIssueAction.setEnabled(false);
        showRuleInfoForIssueAction.setEnabled(false);
        fileOpenedNotifier.init();
    }

    public void setProjectContext(ProjectContext projectContext) {
        this.projectContext = projectContext;
        setName(String.format("SonarQube - %s", ProjectUtils.getInformation(projectContext.getProject()).getDisplayName()));
    }

    public void setSummary(Summary summary) {
        tableSummary.setTreeTableModel(new SummaryModel(summary, !showEmptySeverity.isSelected()));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.RIGHT);
        tableSummary.getColumn(1).setCellRenderer(renderer);
        listIssuesAction.setEnabled(false);
        showRuleInfoAction.setEnabled(false);
    }

    public ActionPlan getSelectedActionPlan() {
        Enumeration<AbstractButton> elements = actionPlanGroup.getElements();
        while (elements.hasMoreElements()) {
            JRadioButtonMenuItem item = (JRadioButtonMenuItem) elements.nextElement();
            if (item.isSelected() && item.getClientProperty(ACTION_PLAN_PROPERTY) instanceof ActionPlan) {
                return (ActionPlan) item.getClientProperty(ACTION_PLAN_PROPERTY);
            }
        }
        return null;
    }

    public void setIssuesContainer(IssuesContainer issuesContainer) {
        this.issuesContainer = issuesContainer;
        if (issuesContainer instanceof SonarRunnerResult) {
            setActionPlans(Collections.<ActionPlan>emptyList());
        }
    }

    public void setProjectKeyChecker(IssueLocation.ProjectKeyChecker projectKeyChecker) {
        this.projectKeyChecker = projectKeyChecker;
    }
    
    public void setActionPlans(List<ActionPlan> plans) {
        dropDownMenu.removeAll();
        JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(org.openide.util.NbBundle.getMessage(Bundle.class, "SonarIssuesTopComponent.actionPlansCombo.none"));
        menuItem.setSelected(true);
        menuItem.addActionListener(actionPlanItemListener);
        menuItem.putClientProperty(ACTION_PLAN_PROPERTY, null);
        actionPlanGroup.add(menuItem);
        dropDownMenu.add(menuItem);
        for (ActionPlan plan : plans) {
            menuItem = new JRadioButtonMenuItem(plan.name());
            menuItem.putClientProperty(ACTION_PLAN_PROPERTY, plan);
            menuItem.addActionListener(actionPlanItemListener);
            actionPlanGroup.add(menuItem);
            dropDownMenu.add(menuItem);
        }
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
        actionPlanGroup = new javax.swing.ButtonGroup();
        tabbedPane = new javax.swing.JTabbedPane();
        summaryPanel = new javax.swing.JPanel();
        sidebar = new javax.swing.JPanel();
        buttonListIssues = new javax.swing.JButton();
        buttonRuleInfo = new javax.swing.JButton();
        showEmptySeverity = new javax.swing.JToggleButton();
        jButton1 = DropDownButtonFactory.createDropDownButton(new javax.swing.ImageIcon(getClass().getResource("/qubexplorer/ui/images/page_gear.png")), dropDownMenu);
        jScrollPane1 = new javax.swing.JScrollPane();
        tableSummary = new org.jdesktop.swingx.JXTreeTable();
        tableSummary.getTableHeader().setReorderingAllowed(false);
        tableSummary.setTreeCellRenderer(new SummaryTreeCellRenderer());
        tableSummary.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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

        summaryPanel.setLayout(new java.awt.BorderLayout());

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

        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/qubexplorer/ui/images/page_gear.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(jButton1, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jButton1.text")); // NOI18N
        jButton1.setToolTipText(org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jButton1.toolTipText")); // NOI18N
        jButton1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jButton1.setBorderPainted(false);

        javax.swing.GroupLayout sidebarLayout = new javax.swing.GroupLayout(sidebar);
        sidebar.setLayout(sidebarLayout);
        sidebarLayout.setHorizontalGroup(
            sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 32, Short.MAX_VALUE)
            .addComponent(showEmptySeverity, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(buttonRuleInfo, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(buttonListIssues, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        sidebarLayout.setVerticalGroup(
            sidebarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sidebarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(buttonListIssues)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonRuleInfo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showEmptySeverity)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        summaryPanel.add(sidebar, java.awt.BorderLayout.LINE_START);

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

        summaryPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        tabbedPane.addTab(org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.summaryPanel.TabConstraints.tabTitle"), summaryPanel); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 648, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 413, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void issuesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_issuesTableMouseClicked
        if (evt.isPopupTrigger()) {
            int row = issuesTable.rowAtPoint(evt.getPoint());
            if (row != -1) {
                issuesTable.changeSelection(row, row, false, false);
                issuesPopupMenu.show(issuesTable, evt.getX(), evt.getY());
            }
            return;
        }
        if (evt.getClickCount() == 2) {
            int row = issuesTable.rowAtPoint(evt.getPoint());
            if (row != -1) {
                if (issuesTable.getSelectedRow() != row) {
                    issuesTable.changeSelection(row, row, false, false);
                }
                if (gotoIssueAction.isEnabled()) {
                    gotoIssueAction.actionPerformed(new ActionEvent(issuesTable, Event.ACTION_EVENT, "Go to Source"));
                }
            }
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
            Summary summary = ((SummaryModel) tableSummary.getTreeTableModel()).getSummary();
            int count;
            if (selectedNode instanceof Summary) {
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
        if (evt.isPopupTrigger()) {
            int row = issuesTable.rowAtPoint(evt.getPoint());
            if (row != -1) {
                issuesTable.changeSelection(row, row, false, false);
                issuesPopupMenu.show(issuesTable, evt.getX(), evt.getY());
            }
        }
    }//GEN-LAST:event_issuesTableMousePressed

    private void issuesTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_issuesTableMouseReleased
        if (evt.isPopupTrigger()) {
            int row = issuesTable.rowAtPoint(evt.getPoint());
            if (row != -1) {
                issuesTable.changeSelection(row, row, false, false);
                issuesPopupMenu.show(issuesTable, evt.getX(), evt.getY());
            }
        }
    }//GEN-LAST:event_issuesTableMouseReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup actionPlanGroup;
    private javax.swing.JButton buttonListIssues;
    private javax.swing.JButton buttonRuleInfo;
    private javax.swing.JTextField filterText;
    private javax.swing.JPanel issuesPanel;
    private javax.swing.JPopupMenu issuesPopupMenu;
    private org.jdesktop.swingx.JXTable issuesTable;
    private javax.swing.JButton jButton1;
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
        if (getSelectedActionPlan() != null) {
            filters.add(new ActionPlanFilter(getSelectedActionPlan()));
        }
        if (treeTableNode instanceof Severity) {
            filters.add(new SeverityFilter((Severity) treeTableNode));
        } else if (treeTableNode instanceof Rule) {
            filters.add(new RuleFilter((Rule) treeTableNode));
        }
        TaskExecutor.execute(new IssuesTask(projectContext, issuesContainer, filters.toArray(new IssueFilter[0])));
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

    public void setIssues(IssueFilter[] filters, RadarIssue... issues) {
        detachCurrentAnnotations();

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

        builder.append(
                "Number of issues:");
        builder.append(issues.length);

        title.setText(builder.toString());
        issuesTable.getColumnExt("Rule").setVisible(true);
        issuesTable.getColumnExt("Severity").setVisible(false);
        issuesTable.getColumnExt(
                "Project Key").setVisible(false);
        issuesTable.getColumnExt("Full Path").setVisible(false);
        issuesTable.getSelectionModel().addListSelectionListener((ListSelectionEvent lse) -> {
            int row = issuesTable.getSelectedRow();
            gotoIssueAction.setEnabled(row != -1);
            showRuleInfoForIssueAction.setEnabled(row != -1);
        });
        showIssuesCount();
        filterText.setText("");
        addEditorAnnotations(issues);
    }

    private void detachCurrentAnnotations() {
        for (Annotation annotation : attachedAnnotations) {
            annotation.detach();
        }
        attachedAnnotations.clear();
    }

    private void addEditorAnnotations(RadarIssue[] issues) {
        for (RadarIssue issue : issues) {
            try {
                if (issue.line() != null) {
                    tryToAtachEditorAnnotation(issue);
                }
            } catch (DataObjectNotFoundException ex) {
                ;
            }
        }
    }

    private void tryToAtachEditorAnnotation(RadarIssue issue) throws DataObjectNotFoundException {
        IssueLocation issueLocation = issue.getLocation();
        try{
            FileObject fileObject = issueLocation.getFileObject(projectContext, projectKeyChecker);
            if (fileObject != null) {
                if (isFileOpen(fileObject)) {
                    Annotation atachedAnnotation = issue.getLocation().attachAnnotation(issue, fileObject);
                    if (atachedAnnotation != null) {
                        attachedAnnotations.add(atachedAnnotation);
                    }
                } else {
                    fileOpenedNotifier.registerFileOpenedListener(fileObject, new AnnotationAttacher(issue));
                }
            }
        }catch(ProjectNotFoundException ex){
            
        }
    }

    private boolean isFileOpen(FileObject fileObject) throws DataObjectNotFoundException {
        DataObject dataObject = DataObject.find(fileObject);
        Lookup lookup = dataObject.getLookup();
        LineCookie lineCookie = lookup.lookup(LineCookie.class);
        Line.Set lineSet = lineCookie.getLineSet();
        return !lineSet.getLines().isEmpty();
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

    public void showIssues(IssueFilter[] filters, RadarIssue... issues) {
        setIssues(filters, issues);
        if (tabbedPane.getTabCount() == 1) {
            tabbedPane.add("Issues", issuesPanel);
        }
        tabbedPane.setSelectedIndex(1);
        gotoIssueAction.setEnabled(false);
        showRuleInfoForIssueAction.setEnabled(false);
    }

    public void showSummary(Summary summary) {
        setSummary(summary);
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

    public void resetState() {
        fileOpenedNotifier.unregisterCurrentFileOpenedListeners();
        detachCurrentAnnotations();
        SimpleSummary emptySummary = new SimpleSummary();
        showSummary(emptySummary);
    }

    public class AnnotationAttacher implements FileObjectOpenedListener {

        private final RadarIssue radarIssue;
        private boolean attached;

        public AnnotationAttacher(RadarIssue radarIssue) {
            this.radarIssue = radarIssue;
        }

        @Override
        public void fileOpened(final FileObject fileOpened) {
            
                if (!attached) {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            IssueLocation issueLocation = radarIssue.getLocation();
                            Annotation annotation = issueLocation.attachAnnotation(radarIssue, fileOpened);
                            if (annotation != null) {
                                attachedAnnotations.add(annotation);
                                attached = true;
                            }
                        } catch (DataObjectNotFoundException ex) {
                            ;
                        }
                    });
                    
                }
            
        }

    }

}
