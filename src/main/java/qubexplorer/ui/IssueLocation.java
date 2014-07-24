package qubexplorer.ui;

import java.io.File;
import java.io.IOException;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import qubexplorer.MvnModelFactory;

/**
 *
 * @author Victor
 */
public class IssueLocation {
    private final String componentKey;
    private final int lineNumber;

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
    
    public Project getProjectOwner(Project parentProject) throws IOException, XmlPullParserException {
        BasicPomInfo basicPomInfo = getBasicPomInfo(getShortProjectKey());
        Model model = new MvnModelFactory().createModel(parentProject);
        if (model.getGroupId().equals(basicPomInfo.getGroupId()) && model.getArtifactId().equals(basicPomInfo.getArtifactId())) {
            return parentProject;
        }
        FileObject mavenDir = findMvnDir(model, basicPomInfo, model.getGroupId());
        if (mavenDir != null) {
            return FileOwnerQuery.getOwner(mavenDir);
        } else {
            return null;
        }
    }
    

    public File getFile(Project parentProject) throws IOException, XmlPullParserException {
        Project projectOwner = getProjectOwner(parentProject);
        if(projectOwner == null) {
            throw new ProjectNotFoundException(getShortProjectKey());
        }
        File file;
        String filePath = toFilePath(componentKey, ".java");
        if (componentKey.contains("/")) {
            file = new File(projectOwner.getProjectDirectory().getPath(), filePath);
        } else {
            Sources sources = ProjectUtils.getSources(projectOwner);
            SourceGroup[] sourceGroups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
            file = new File(sourceGroups[0].getRootFolder().getPath(), filePath);
        }
        return file;
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
    
    private static String toFilePath(String componentKey, String extension) {
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

}
