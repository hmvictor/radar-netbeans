package qubexplorer.ui;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.JOptionPane;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Annotation;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;
import qubexplorer.IssuesContainer;
import qubexplorer.MvnModelInputException;
import qubexplorer.RadarIssue;
import qubexplorer.filter.IssueFilter;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.editorannotations.InfoAnnotation;
import qubexplorer.ui.task.Task;

/**
 *
 * @author Victor
 */
public class IssuesTask extends Task<List<RadarIssue>>{
    private final IssuesContainer issuesContainer;
    private final IssueFilter[] filters;

    public IssuesTask(ProjectContext projectContext, IssuesContainer issuesContainer, IssueFilter[] filters) {
        super(projectContext, issuesContainer instanceof SonarQube? ((SonarQube)issuesContainer).getServerUrl(): null);
        this.issuesContainer=issuesContainer;
        this.filters=filters;
    }

    @Override
    public List<RadarIssue> execute() {
        return issuesContainer.getIssues(getUserCredentials(), getProjectContext().getConfiguration().getKey(), filters);
    }

    @Override
    protected void success(List<RadarIssue> result) {
        SonarIssuesTopComponent sonarTopComponent = (SonarIssuesTopComponent) WindowManager.getDefault().findTopComponent("SonarIssuesTopComponent");
        sonarTopComponent.open();
        sonarTopComponent.requestVisible();
        sonarTopComponent.setProjectContext(getProjectContext());
        sonarTopComponent.showIssues(filters, result.toArray(new RadarIssue[0]));
        for (RadarIssue radarIssue : result) {
            try {
                if(radarIssue.line() == null) {
                    continue;
                }
                IssueLocation issueLocation = new IssueLocation(radarIssue.componentKey(), radarIssue.line());
                File file = issueLocation.getFile(getProjectContext().getProject(), getProjectContext().getConfiguration());
                FileObject fileObject = FileUtil.toFileObject(file);
                if (fileObject == null) {
                    String messageTitle = org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.unexistentFile.title");
                    String message = MessageFormat.format(org.openide.util.NbBundle.getMessage(SonarIssuesTopComponent.class, "SonarIssuesTopComponent.unexistentFile.text"), file.getPath());
                    JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), message, messageTitle, JOptionPane.WARNING_MESSAGE);
                    return;
                }
                DataObject dataObject = DataObject.find(fileObject);
                LineCookie lineCookie = (LineCookie)dataObject.getLookup().lookup(LineCookie.class);
                Line.Set lineSet = lineCookie.getLineSet();
                int index=Math.min(radarIssue.line(), lineSet.getLines().size())-1;
                final Line line = lineSet.getOriginal(index);
                final Annotation ann = new InfoAnnotation();
                if(line != null) {
                    ann.attach(line);
                }
            } catch (MvnModelInputException | DataObjectNotFoundException ex) {
                ;
            }
        }
        
    }
    
}
