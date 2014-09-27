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
public class MvnModelFactory {
    
    public Model createModel(Project project) throws MvnModelInputException{
        return createModel(project.getProjectDirectory());
    }
    
    public Model createModel(FileObject projectDir) throws MvnModelInputException {
        FileObject pomFile = projectDir.getFileObject("pom.xml");
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try(Reader reader=new InputStreamReader(pomFile.getInputStream())){
            Model model = mavenreader.read(reader);
            model.setPomFile(new File(pomFile.getPath()));
            return model;
        }catch(XmlPullParserException | IOException ex) {
            throw new MvnModelInputException(ex);
        }
    }
    
}
