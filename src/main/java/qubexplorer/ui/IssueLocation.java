package qubexplorer.ui;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
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
    private static final String DEFAULT_EXTENSION=".java";

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
        String extension="";
        char separator;
        if(componentKey.contains("/")) {
            separator='/';
        }else{
            extension=DEFAULT_EXTENSION;
            separator='.';
        }
        int index=componentKey.lastIndexOf(separator);
        if(index < componentKey.length() -1) {
            return componentKey.substring(index+1)+extension;
        }else{
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
        String path = getPath();
        if(path.contains("/")) {
            /* It's a relative file path*/
            file = new File(projectOwner.getProjectDirectory().getPath(), path);
        }else{
            /* It's an element name. Assume is a java file */
            String filePath=path.replace(".", "/")+".java";
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
    
    public static class IssueLocationComparator implements Comparator<IssueLocation>{
        
        
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
