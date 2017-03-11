package qubexplorer.ui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Annotation;
import org.openide.text.Line;
import org.openide.util.Lookup;
import qubexplorer.ProjectNotFoundException;
import qubexplorer.RadarIssue;
import qubexplorer.ui.issues.FileObjectOpenedListener;
import qubexplorer.ui.issues.IssueLocation;

/**
 *
 * @author Víctor
 */
public class IssueEditorAnnotationAttacher {
    
    private final List<Annotation> attachedAnnotations = new CopyOnWriteArrayList<>();
    private final FileOpenedNotifier fileOpenedNotifier = new FileOpenedNotifier();
    private ProjectContext projectContext;
    private IssueLocation.ProjectKeyChecker projectKeyChecker;
    private boolean attached;

    public void init() {
        fileOpenedNotifier.init();
    }

    public void setProjectContext(ProjectContext projectContext) {
        this.projectContext = projectContext;
    }

    public void setProjectKeyChecker(IssueLocation.ProjectKeyChecker projectKeyChecker) {
        this.projectKeyChecker = projectKeyChecker;
    }

    public boolean isAttached() {
        return attached;
    }
    
    public void attachAnnotations(RadarIssue[] issues) {
        attached=true;
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
        try {
            FileObject fileObject = issueLocation.getFileObject(projectContext, projectKeyChecker);
            if (fileObject != null) {
                if (isFileOpen(fileObject)) {
                    Annotation atachedAnnotation = issue.getLocation().attachAnnotation(issue, fileObject);
                    if (atachedAnnotation != null) {
                        attachedAnnotations.add(atachedAnnotation);
                    }
                } else {
                    fileOpenedNotifier.registerFileOpenedListener(fileObject, new FileAnnotationAttacher(issue));
                }
            }
        } catch (ProjectNotFoundException ex) {
        }
    }

    private boolean isFileOpen(FileObject fileObject) throws DataObjectNotFoundException {
        DataObject dataObject = DataObject.find(fileObject);
        Lookup lookup = dataObject.getLookup();
        LineCookie lineCookie = lookup.lookup(LineCookie.class);
        Line.Set lineSet = lineCookie.getLineSet();
        return !lineSet.getLines().isEmpty();
    }

    public void detachAnnotations() {
        attached=false;
        fileOpenedNotifier.unregisterCurrentFileOpenedListeners();
        attachedAnnotations.forEach((annotation) -> {
            annotation.detach();
        });
        attachedAnnotations.clear();
    }

    public class FileAnnotationAttacher implements FileObjectOpenedListener {

        private final RadarIssue issue;
        private boolean attached;

        public FileAnnotationAttacher(RadarIssue issue) {
            this.issue = issue;
        }

        @Override
        public void fileOpened(final FileObject fileOpened) {
            if (!attached) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        IssueLocation issueLocation = issue.getLocation();
                        Annotation attachedAnnotation = issueLocation.attachAnnotation(issue, fileOpened);
                        if (attachedAnnotation != null) {
                            attachedAnnotations.add(attachedAnnotation);
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
