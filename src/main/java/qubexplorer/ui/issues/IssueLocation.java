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
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Annotation;
import org.openide.text.Line;
import org.openide.util.Lookup;
import qubexplorer.RadarIssue;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.SonarQubeProjectBuilder;

/**
 *
 * @author Victor
 */
public class IssueLocation {

    private final String componentKey;
    private final int lineNumber;
    private static final String DEFAULT_EXTENSION = ".java";

    public IssueLocation(String componentKey, int lineNumber) {
        this.componentKey = componentKey;
        this.lineNumber = lineNumber;
    }

    public String getPath() {
        String path = componentKey;
        int index = path.lastIndexOf(':');
        if (index != -1) {
            path = path.substring(index + 1);
        }
        return path;
    }

    public String getName() {
        String extension = "";
        char separator;
        if (componentKey.contains("/")) {
            separator = '/';
        } else {
            extension = DEFAULT_EXTENSION;
            separator = '.';
        }
        int index = componentKey.lastIndexOf(separator);
        if (index < componentKey.length() - 1) {
            return componentKey.substring(index + 1) + extension;
        } else {
            return "";
        }
    }

    public String getComponentKey() {
        return componentKey;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public String getProjectKey() {
        String path = componentKey;
        int index = path.lastIndexOf(':');
        if (index != -1) {
            path = path.substring(0, index);
        }
        return path;
    }

    public String getShortProjectKey() {
        String[] tokens = componentKey.split(":");
        assert tokens.length >= 2;
        return tokens[0] + ":" + tokens[1];
    }

    public Project getProjectOwner(Project parentProject, SonarQubeProjectConfiguration projectConfiguration) {
        FileObject projectDir = findProjectDir(parentProject, projectConfiguration, getShortProjectKey());
        if (projectDir != null) {
            return FileOwnerQuery.getOwner(projectDir);
        } else {
            return null;
        }
    }

    public File getFile(Project parentProject, SonarQubeProjectConfiguration projectConfiguration) {
        Project projectOwner = getProjectOwner(parentProject, projectConfiguration);
        if (projectOwner == null) {
            throw new ProjectNotFoundException(getShortProjectKey());
        }
        File file;
        String path = getPath();
        if (path.contains("/")) {
            /* It's a relative file path*/
            file = new File(projectOwner.getProjectDirectory().getPath(), path);
        } else {
            /* It's an element name. Assume is a java file */
            String filePath = path.replace(".", "/") + DEFAULT_EXTENSION;
            Sources sources = ProjectUtils.getSources(projectOwner);
            SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
            file = new File(sourceGroups[0].getRootFolder().getPath(), filePath);
        }
        return file;
    }
    
    public FileObject getFileObject(Project parentProject, SonarQubeProjectConfiguration projectConfiguration) {
        File file=getFile(parentProject, projectConfiguration);
        return FileUtil.toFileObject(file);
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
            return componentKey;
        } else {
            return componentKey + " [" + lineNumber + "]";
        }
    }
    
    public static EditorCookie getEditorCookie(FileObject fileObject) throws DataObjectNotFoundException{
        DataObject dataObject = DataObject.find(fileObject);
        Lookup lookup = dataObject.getLookup();
        return (EditorCookie) lookup.lookup(LineCookie.class);
    }

    static BasicPomInfo getBasicPomInfo(String componentKey) {
        String[] tokens = componentKey.split(":");
        assert tokens.length >= 2;
        return new BasicPomInfo(tokens[0], tokens[1]);
    }

    private static FileObject findProjectDir(Project project, SonarQubeProjectConfiguration projectConfiguration, String key) {
        if (projectConfiguration.getKey().toString().equals(key)) {
            return project.getProjectDirectory();
        }
        Set<Project> subprojects = ProjectUtils.getContainedProjects(project, true);
        if (subprojects != null) {
            for (Project subproject : subprojects) {
                SonarQubeProjectConfiguration subprojectInfo = SonarQubeProjectBuilder.getSubconfiguration(projectConfiguration, subproject);
                if (subprojectInfo.getKey().toString().equals(key)) {
                    return subproject.getProjectDirectory();
                }
            }
        }
        return null;
    }

    private static class BasicPomInfo {

        private final String groupId;
        private final String artifactId;

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

    public static class IssueLocationComparator implements Comparator<IssueLocation> {

        @Override
        public int compare(IssueLocation t, IssueLocation t1) {
            int result = t.getPath().compareTo(t1.getPath());
            if (result != 0) {
                return result;
            } else {
                return Integer.compare(t.getLineNumber(), t1.getLineNumber());
            }
        }

    }

}
