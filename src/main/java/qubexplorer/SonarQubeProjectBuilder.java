package qubexplorer;

import java.util.Map;
import java.util.prefs.Preferences;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Victor
 */
public final class SonarQubeProjectBuilder {
    
    private SonarQubeProjectBuilder() {
        
    }
    
    private static boolean isMvnProject(Project project) {
        FileObject pomFile = project.getProjectDirectory().getFileObject("pom.xml");
        return pomFile != null;
    }
    
    public static SonarQubeProjectConfiguration getDefaultConfiguration(Project project) {
        if(isMvnProject(project)) {
            try {
                return new SonarMvnProject(project);
            } catch (MvnModelInputException ex) {
                throw new SonarQubeProjectException(ex);
            }
        }else{
            String name = ProjectUtils.getInformation(project).getName();
            return new GenericSonarQubeProjectConfiguration(name, new ResourceKey("base", name), "1.0");
        }
    }
    
    public static SonarQubeProjectConfiguration getConfiguration(Project project) {
        if(isMvnProject(project)) {
            try {
                return new SonarMvnProject(project);
            } catch (MvnModelInputException ex) {
                throw new SonarQubeProjectException(ex);
            }
        }else{
            Preferences preferences = ProjectUtils.getPreferences(project, SonarQubeProjectConfiguration.class, true);
            String name = ProjectUtils.getInformation(project).getName();
            String key=preferences.get("key", null);
            String version=preferences.get("1.0", null);
            if(key == null || version == null) {
                //ask for properties
                Map<String, String> properties=null;
                if(properties != null) {
                    key=properties.get("key");
                    version=properties.get("version");
                }else{
                    return null;
                }
            }
            return new GenericSonarQubeProjectConfiguration(name, ResourceKey.valueOf(key), version);
        }
    }
    
    public static SonarQubeProjectConfiguration getSubconfiguration(SonarQubeProjectConfiguration parentConfig, Project subproject) {
        FileObject pomFile = subproject.getProjectDirectory().getFileObject("pom.xml");
        if(pomFile != null) {
            try {
                return new SonarMvnProject(subproject);
            } catch (MvnModelInputException ex) {
                throw new SonarQubeProjectException(ex);
            }
        }else{
            String name = ProjectUtils.getInformation(subproject).getName();
            return new GenericSonarQubeProjectConfiguration(name, new ResourceKey(parentConfig.getKey().getPart(0), name), parentConfig.getVersion());
        }
    }
    
}
