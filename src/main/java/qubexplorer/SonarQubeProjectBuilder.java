package qubexplorer;

import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Victor
 */
public class SonarQubeProjectBuilder {
    
    public static SonarQubeProject create(Project project) {
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
