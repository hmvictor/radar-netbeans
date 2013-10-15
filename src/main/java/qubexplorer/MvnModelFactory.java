package qubexplorer;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Victor
 */
public class MvnModelFactory {
    
    public Model createModel(Project project) throws IOException, XmlPullParserException {
        return createModel(project.getProjectDirectory().getFileObject("pom.xml"));
    }
    
    public Model createModel(FileObject pomFile) throws IOException, XmlPullParserException {
        Model model;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        model = mavenreader.read(new InputStreamReader(pomFile.getInputStream()));
        model.setPomFile(new File(pomFile.getPath()));
        return model;
    }
    
}
