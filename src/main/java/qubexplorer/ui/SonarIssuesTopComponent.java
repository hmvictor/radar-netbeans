package qubexplorer.ui;

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
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultRowSorter;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
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
import org.openide.util.NbPreferences;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.issue.ActionPlan;
import org.sonar.wsclient.issue.Issue;
import org.sonar.wsclient.services.Rule;
import qubexplorer.RadarIssue;
import qubexplorer.IssuesContainer;
import qubexplorer.MvnModelFactory;
import qubexplorer.Severity;
import qubexplorer.server.SonarQube;
import qubexplorer.Summary;
import qubexplorer.filter.ActionPlanFilter;
import qubexplorer.filter.IssueFilter;
import qubexplorer.filter.RuleFilter;
import qubexplorer.filter.SeverityFilter;
import qubexplorer.runner.SonarRunnerResult;
import qubexplorer.ui.options.SonarQubeOptionsPanel;

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
    "CTL_SonarIssuesTopComponent=Sonar - Issues",
    "HINT_SonarIssuesTopComponent=This is a Sonar window"
})
public final class SonarIssuesTopComponent extends TopComponent {

    private IssuesContainer issuesContainer;
    private Project project;
    private Issue[] issues;

    private final Comparator<Severity> severityComparator = Collections.reverseOrder(new Comparator<Severity>() {
        @Override
        public int compare(Severity t, Severity t1) {
            return t.compareTo(t1);
        }
    });

    private final Comparator<Location> locationComparator = new Comparator<Location>() {
        @Override
        public int compare(Location t, Location t1) {
            int result = t.component.compareTo(t1.component);
            if (result != 0) {
                return result;
            } else {
                return Integer.compare(t.lineNumber, t1.lineNumber);
            }
        }
    };

    public SonarIssuesTopComponent() {
        initComponents();
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
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setSummary(Summary summary) {
        tableSummary.setTreeTableModel(new SummaryModel(summary));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(JLabel.RIGHT);
        tableSummary.getColumn(1).setCellRenderer(renderer);
    }

    public void setIssuesContainer(IssuesContainer issuesContainer) {
        this.issuesContainer = issuesContainer;
    }

    public void setActionPlans(List<ActionPlan> plans) {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement(org.openide.util.NbBundle.getMessage(Bundle.class, "SonarIssuesTopComponent.actionPlansCombo.none"));
        for (ActionPlan plan : plans) {
            model.addElement(plan);
        }
        actionPlansCombo.setModel(model);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        tabbedPane = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tableSummary = new org.jdesktop.swingx.JXTreeTable();
        tableSummary.getTableHeader().setReorderingAllowed(false);
        tableSummary.setTreeCellRenderer(new SummaryTreeCellRenderer());
        panelTop = new javax.swing.JPanel();
        actionPlansCombo = new javax.swing.JComboBox();
        actionPlansCombo.setRenderer(new ActionPlansRenderer());
        jLabel4 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        title = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        issuesTable = new org.jdesktop.swingx.JXTable();
        filterText = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        shownCount = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jLabel2.text")); // NOI18N

        tableSummary.setRootVisible(true);
        tableSummary.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableSummaryMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tableSummary);

        actionPlansCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                actionPlansComboActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel4, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jLabel4.text")); // NOI18N

        javax.swing.GroupLayout panelTopLayout = new javax.swing.GroupLayout(panelTop);
        panelTop.setLayout(panelTopLayout);
        panelTopLayout.setHorizontalGroup(
            panelTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTopLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(actionPlansCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        panelTopLayout.setVerticalGroup(
            panelTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTopLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelTopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(actionPlansCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
                    .addComponent(panelTop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(panelTop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 359, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabbedPane.addTab(org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jPanel2.TabConstraints.tabTitle"), jPanel2); // NOI18N

        title.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        org.openide.awt.Mnemonics.setLocalizedText(title, org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.title.text")); // NOI18N

        issuesTable.setModel(

            new javax.swing.table.DefaultTableModel(
                new Object [][] {

                },
                new String [] {
                    "", "Message", "Severity", "Location", "MvnId", "Rule"
                }
            ) {
                Class[] types = new Class [] {
                    Severity.class

                    , java.lang.String.class

                    , Severity.class

                    , java.lang.Object.class

                    , java.lang.String.class

                    , java.lang.String.class
                };
                boolean[] canEdit = new boolean[]{
                    false, false, false, false, false, false
                };

                public Class getColumnClass(int columnIndex) {
                    return types[columnIndex];
                }

                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return canEdit[columnIndex];
                }

            });
            issuesTable.setColumnControlVisible(true);
            issuesTable.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    issuesTableMouseClicked(evt);
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

            javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
            jPanel1.setLayout(jPanel1Layout);
            jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE)
                        .addComponent(title, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(filterText)
                            .addGap(18, 18, 18)
                            .addComponent(jLabel3)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(shownCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
            );
            jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(title)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 341, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(filterText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(shownCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel3))
                    .addContainerGap())
            );

            tabbedPane.addTab(org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.jPanel1.TabConstraints.tabTitle"), jPanel1); // NOI18N

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
        if (evt.getClickCount() == 2) {
            int row = issuesTable.rowAtPoint(evt.getPoint());
            if (row != -1) {
                row = issuesTable.getRowSorter().convertRowIndexToModel(row);
                try {
                    String shortKey = removeBranchPart(issues[row].componentKey());
                    Project p = findProject(project, getBasicPomInfo(shortKey));
                    if (p != null) {
                        String componentKey = issues[row].componentKey();
                        File file;
                        if (componentKey.contains("/")) {
                            file = new File(p.getProjectDirectory().getPath(), toPath(componentKey, ".java"));
                        } else {
                            Sources sources = ProjectUtils.getSources(p);
                            SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
                            file = new File(sourceGroups[0].getRootFolder().getPath(), toPath(componentKey, ".java"));
                        }
                        if (issues[row].line() == null) {
                            openFile(file, 1);
                        } else {
                            openFile(file, issues[row].line());
                        }
                    } else {
                        String message = org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "ProjectNotFound", shortKey);
                        DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE));
                    }
                } catch (IOException | XmlPullParserException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }//GEN-LAST:event_issuesTableMouseClicked

    private void tableSummaryMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableSummaryMouseClicked
        if (evt.getClickCount() != 2) {
            return;
        }
        int rowIndex = tableSummary.rowAtPoint(evt.getPoint());
        if (rowIndex < 0) {
            return;
        }
        Object selectedNode = tableSummary.getPathForRow(rowIndex).getLastPathComponent();
        int column = tableSummary.convertColumnIndexToModel(tableSummary.columnAtPoint(evt.getPoint()));
        if (column == 1) {
            IssueFilter[] filters;
            if (selectedNode instanceof Summary) {
                filters = new IssueFilter[0];
            } else if (selectedNode instanceof Severity) {
                filters = new IssueFilter[]{new SeverityFilter((Severity) selectedNode)};
            } else {
                assert selectedNode instanceof Rule;
                filters = new IssueFilter[]{new RuleFilter((Rule) selectedNode)};
            }
            try {
                new IssuesWorker(issuesContainer, project, SonarQube.toResource(project), filters).execute();
            } catch (IOException | XmlPullParserException ex) {
                Exceptions.printStackTrace(ex);
            }
        } else if (selectedNode instanceof Rule) {
            Rule rule = (Rule) selectedNode;
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox actionPlansCombo;
    private javax.swing.JTextField filterText;
    private org.jdesktop.swingx.JXTable issuesTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel panelTop;
    private javax.swing.JTextField shownCount;
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
        DefaultTableModel model = (DefaultTableModel) issuesTable.getModel();
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
        for (RadarIssue issue : issues) {
            String name = toPath(issue.componentKey(), ".java");
            String mvnId = toMvnId(issue.componentKey());
            model.addRow(new Object[]{issue.severityObject(), issue.message(), issue.severityObject(), new Location(name, issue.line()), mvnId, issue.rule().getTitle()});
        }
        this.issues = issues;
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
        issuesTable.getColumnExt("Severity").setVisible(true);
        issuesTable.getColumnExt("Rule").setVisible(true);
        showIssuesCount();
    }

    public static String toPath(String componentKey, String extension) {
        String path = componentKey;
        int index = path.lastIndexOf(':');
        if (index != -1) {
            path = path.substring(index + 1);
        }
        if (!path.contains("/")) {
            path = path.replace(".", "/") + extension;
        }
        return path;
    }

    public String toMvnId(String componentKey) {
        String path = componentKey;
        int index = path.lastIndexOf(':');
        if (index != -1) {
            path = path.substring(0, index);
        }
        return path;
    }

    private static BasicPomInfo getBasicPomInfo(String componentKey) {
        String[] tokens = componentKey.split(":");
        assert tokens.length >= 2;
        return new BasicPomInfo(tokens[0], tokens[1]);
    }

    private static FileObject findMvnDir(Model model, BasicPomInfo basicPomInfo, String groupId) throws IOException, XmlPullParserException {
        MvnModelFactory factory = new MvnModelFactory();
        for (String module : model.getModules()) {
            FileObject moduleFile = FileUtil.toFileObject(new File(model.getProjectDirectory(), module));
            Model m = factory.createModel(moduleFile);
            String tmpGroupId = m.getGroupId() == null ? groupId : m.getGroupId();
            if (tmpGroupId.equals(basicPomInfo.getGroupId()) && m.getArtifactId().equals(basicPomInfo.getArtifactId())) {
                return moduleFile;
            } else {
                FileObject o = findMvnDir(m, basicPomInfo, tmpGroupId);
                if (o != null) {
                    return o;
                }
            }
        }
        return null;
    }

    private static Project findProject(Project project, BasicPomInfo basicPomInfo) throws IOException, XmlPullParserException {
        Model model = new MvnModelFactory().createModel(project);
        if (model.getGroupId().equals(basicPomInfo.getGroupId()) && model.getArtifactId().equals(basicPomInfo.getArtifactId())) {
            return project;
        }
        FileObject mavenDir = findMvnDir(model, basicPomInfo, model.getGroupId());
        if (mavenDir != null) {
            return FileOwnerQuery.getOwner(mavenDir);
        } else {
            return null;
        }
    }

    private void showIssuesCount() {
        NumberFormat format = NumberFormat.getIntegerInstance();
        shownCount.setText(format.format(issuesTable.getRowSorter().getViewRowCount()));
    }

    private String removeBranchPart(String componentKey) {
        String[] tokens = componentKey.split(":");
        assert tokens.length >= 2;
        return tokens[0] + ":" + tokens[1];
    }

    public void showIssuesList() {
        tabbedPane.setSelectedIndex(1);
    }

    public void showSummary() {
        tabbedPane.setSelectedIndex(0);
//        if(issuesContainer instanceof SonarQube) {
//            panelTop.setVisible(true);
//            String serverUrl = NbPreferences.forModule(SonarQubeOptionsPanel.class).get("address", "http://localhost:9000");
//            try {
//                new ActionPlansWorker(serverUrl, SonarQube.toResource(project)).execute();
//            } catch (IOException | XmlPullParserException ex) {
//                Exceptions.printStackTrace(ex);
//            }
//        }else{
//            panelTop.setVisible(false);
//        }
    }

    private static class BasicPomInfo {

        private String groupId;
        private String artifactId;

        public BasicPomInfo(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }
    }

    public class Location {

        private String component;
        private Integer lineNumber;

        public Location(String component, Integer lineNumber) {
            this.component = component;
            this.lineNumber = lineNumber;
        }

        public String getComponent() {
            return component;
        }

        public Integer getLineNumber() {
            return lineNumber;
        }

        @Override
        public String toString() {
            if (lineNumber == null) {
                return component;
            } else {
                return component + " [" + lineNumber + "]";
            }
        }
    }

}
