package qubexplorer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Victor
 */
public class SonarQubeProjectInfoBuilder {
    
    public static SonarQubeProjectInfo create(Project project) {
        FileObject pomFile = project.getProjectDirectory().getFileObject("pom.xml");
        if(pomFile != null) {
            try {
                return new SonarMvnProject(project);
            } catch (MvnModelInputException ex) {
                throw new SonarQubeProjectException(ex);
            }
        }else{
            return new SimpleSonarProject(project);
        }
    }
    
}
