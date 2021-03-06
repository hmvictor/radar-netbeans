package qubexplorer.ui.issues;

import qubexplorer.ProjectNotFoundException;
import java.io.File;
import java.util.Comparator;
import java.util.Set;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Annotation;
import org.openide.text.Line;
import org.openide.util.Lookup;
import qubexplorer.RadarIssue;
import qubexplorer.ResourceKey;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.ui.ProjectContext;

/**
 *
 * @author Victor
 */
public class IssueLocation {

    private final ResourceKey componentKey;
    private final int lineNumber;
    private static final String DEFAULT_EXTENSION = ".java";

    public IssueLocation(String componentKey, int lineNumber) {
        this.lineNumber = lineNumber;
        this.componentKey = ResourceKey.valueOf(componentKey);
    }

    public String getComponentPath() {
        return componentKey.getLastPart();
    }

    public String getSimpleComponentName() {
        String extension = "";
        char pathSeparator;
        String componentPath = getComponentPath();
        if (componentPath.contains("/")) {
            pathSeparator = '/';
        } else {
            extension = DEFAULT_EXTENSION;
            pathSeparator = '.';
        }
        int index = componentPath.lastIndexOf(pathSeparator);
        if (index < componentPath.length() - 1) {
            return componentPath.substring(index + 1) + extension;
        } else {
            return "";
        }
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public ResourceKey getProjectKey() {
        return componentKey.subkey(0, componentKey.getPartsCount() - 1);
    }

    public Project getProjectOwner(ProjectContext projectContext, ProjectKeyChecker projectKeyChecker) {
        FileObject projectDir = findProjectDir(projectContext, getProjectKey(), projectKeyChecker);
        if (projectDir != null) {
            return FileOwnerQuery.getOwner(projectDir);
        } else {
            return null;
        }
    }

    public File getFile(ProjectContext projectContext, ProjectKeyChecker projectKeyChecker) {
        Project projectOwner = getProjectOwner(projectContext, projectKeyChecker);
        if (projectOwner == null) {
            throw new ProjectNotFoundException(getProjectKey().toString());
        }
        File file;
        String componentPath = getComponentPath();
        if (isFilePath(componentPath)) {
            /* It's a relative file path. Relative to project directory?
            Example: src/main/java/victor/simpleproject/Persona.java */
            // Use project.directory or sources group? 
            file = new File(projectOwner.getProjectDirectory().getPath(), componentPath);
        } else {
            /* 
                It's an element name. Assume is a java file. 
                Example: package.subpackage.ClassA 
             */
            String filePath = componentPath.replace(".", "/") + DEFAULT_EXTENSION;
            Sources sources = ProjectUtils.getSources(projectOwner);
            SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
            file = new File(sourceGroups[0].getRootFolder().getPath(), filePath);
        }
        return file;
    }

    private static boolean isFilePath(String componentPath) {
        return componentPath.contains("/");
    }

    public FileObject getFileObject(ProjectContext projectContext, ProjectKeyChecker projectKeyChecker) {
        return FileUtil.toFileObject(getFile(projectContext, projectKeyChecker));
    }

    public Annotation attachAnnotation(RadarIssue radarIssue, FileObject fileObject) throws DataObjectNotFoundException {
        Annotation ann = null;
        EditorCookie editorCookie = getEditorCookie(fileObject);
        if (editorCookie != null) {
            Line line = getLine(editorCookie);
            if (line != null) {
                ann = new SonarQubeEditorAnnotation(radarIssue.severityObject(), radarIssue.message());
                ann.attach(line);
            }
        }
        return ann;
    }

    public Line getLine(EditorCookie editorCookie) {
        Line.Set lineSet = editorCookie.getLineSet();
        int effectiveLineNumber = getLineNumber() <= 0 ? 1 : getLineNumber();
        int index = Math.min(effectiveLineNumber, lineSet.getLines().size()) - 1;
        return lineSet.getCurrent(index);
    }

    @Override
    public String toString() {
        if (lineNumber <= 0) {
            return componentKey.toString();
        } else {
            return componentKey.toString() + " [" + lineNumber + "]";
        }
    }

    public static EditorCookie getEditorCookie(FileObject fileObject) throws DataObjectNotFoundException {
        DataObject dataObject = DataObject.find(fileObject);
        Lookup lookup = dataObject.getLookup();
        return lookup.lookup(EditorCookie.class);
    }

    private static FileObject findProjectDir(ProjectContext projectContext, ResourceKey issueProjectKey, ProjectKeyChecker projectKeyChecker) {
        if (projectKeyChecker.equals(projectContext.getConfiguration(), issueProjectKey, false) ) { 
            return projectContext.getProject().getProjectDirectory();
        }
        Set<Project> subprojects = ProjectUtils.getContainedProjects(projectContext.getProject(), true);
        if (subprojects != null) {
            for (Project subproject : subprojects) {
                SonarQubeProjectConfiguration subprojectInfo = projectContext.getConfiguration().createConfiguration(subproject);
                if (projectKeyChecker.equals(subprojectInfo, issueProjectKey, true) ) {
                    return subproject.getProjectDirectory();
                }
            }
        }
        return null;
    }

    public static class IssueLocationComparator implements Comparator<IssueLocation> {

        @Override
        public int compare(IssueLocation t, IssueLocation t1) {
            int result = t.getComponentPath().compareTo(t1.getComponentPath());
            if (result != 0) {
                return result;
            } else {
                return Integer.compare(t.getLineNumber(), t1.getLineNumber());
            }
        }

    }

    public static interface ProjectKeyChecker {

        boolean equals(SonarQubeProjectConfiguration configuration, ResourceKey projectKeyIssue, boolean isSubmodule);

    }

}
