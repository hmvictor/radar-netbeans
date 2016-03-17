package qubexplorer.ui;

import java.io.File;
import java.util.Comparator;
import java.util.Set;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Annotation;
import org.openide.text.Line;
import org.openide.util.Lookup;
import qubexplorer.MvnModelInputException;
import qubexplorer.RadarIssue;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.SonarQubeProjectBuilder;
import qubexplorer.ui.editorannotations.SonarQubeAnnotation;

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

    public IssueLocation(String componentKey) {
        this(componentKey, 0);
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

    public Project getProjectOwner(Project parentProject, SonarQubeProjectConfiguration projectConfiguration) throws MvnModelInputException {
        FileObject projectDir = findProjectDir(parentProject, projectConfiguration, getShortProjectKey());
        if (projectDir != null) {
            return FileOwnerQuery.getOwner(projectDir);
        } else {
            return null;
        }
    }

    public File getFile(Project parentProject, SonarQubeProjectConfiguration projectConfiguration) throws MvnModelInputException {
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

    public Annotation attachAnnotation(RadarIssue radarIssue, FileObject fileObject) throws DataObjectNotFoundException {
        DataObject dataObject = DataObject.find(fileObject);
        Lookup lookup = dataObject.getLookup();
        LineCookie lineCookie = (LineCookie) lookup.lookup(LineCookie.class);
        Line.Set lineSet = lineCookie.getLineSet();
        int index = Math.min(getLineNumber(), lineSet.getLines().size()) - 1;
        Line line = lineSet.getCurrent(index);
        Annotation ann = null;
        if (line != null) {
            ann = new SonarQubeAnnotation(radarIssue.severityObject(), radarIssue.message());
            ann.attach(line);
        }
        return ann;
    }


    @Override
    public String toString() {
        if (lineNumber <= 0) {
            return componentKey;
        } else {
            return componentKey + " [" + lineNumber + "]";
        }
    }

    static BasicPomInfo getBasicPomInfo(String componentKey) {
        String[] tokens = componentKey.split(":");
        assert tokens.length >= 2;
        return new BasicPomInfo(tokens[0], tokens[1]);
    }

    private static FileObject findProjectDir(Project project, SonarQubeProjectConfiguration projectConfiguration, String key) throws MvnModelInputException {
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
