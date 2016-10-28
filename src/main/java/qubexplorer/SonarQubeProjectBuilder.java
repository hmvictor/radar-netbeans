package qubexplorer;

import java.io.IOException;
import java.util.Map;
import java.util.prefs.Preferences;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;

/**
 *
 * @author Victor
 */
public final class SonarQubeProjectBuilder {

    private SonarQubeProjectBuilder() {

    }

    public static SonarQubeProjectConfiguration getDefaultConfiguration(Project project) {
        if (SonarMvnProject.isMvnProject(project)) {
            try {
                return new SonarMvnProject(project);
            } catch (MvnModelInputException ex) {
                throw new SonarQubeProjectException(ex);
            }
        } else {
            try {
                return new PlainDirectoryProject(project);
//            String name = ProjectUtils.getInformation(project).getName();
//            return new GenericSonarQubeProjectConfiguration(name, new ResourceKey("base", name), "1.0");
            } catch (IOException ex) {
                throw new SonarQubeProjectException(ex);
            }
        }
    }

    public static SonarQubeProjectConfiguration createConfigurationForSubproject(SonarQubeProjectConfiguration parentProjectConfig, Project subproject) {
        if (SonarMvnProject.isMvnProject(subproject)) {
            try {
                return new SonarMvnProject(subproject);
            } catch (MvnModelInputException ex) {
                throw new SonarQubeProjectException(ex);
            }
        } else {
            try {
//            return new GenericSonarQubeProjectConfiguration(name, new ResourceKey(parentConfig.getKey().getPart(0), name), parentConfig.getVersion());
                return new PlainDirectoryProject(subproject);
            } catch (IOException ex) {
                throw new SonarQubeProjectException(ex);
            }
        }
    }

    public static SonarQubeProjectConfiguration getConfiguration(Project project) {
        if (SonarMvnProject.isMvnProject(project)) {
            try {
                return new SonarMvnProject(project);
            } catch (MvnModelInputException ex) {
                throw new SonarQubeProjectException(ex);
            }
        } else {
            Preferences preferences = ProjectUtils.getPreferences(project, SonarQubeProjectConfiguration.class, true);
            String name = ProjectUtils.getInformation(project).getName();
            String key = preferences.get("key", null);
            String version = preferences.get("1.0", null);
            if (key == null || version == null) {
                //ask for properties
                Map<String, String> properties = null;
                if (properties != null) {
                    key = properties.get("key");
                    version = properties.get("version");
                } else {
                    return null;
                }
            }
            return new GenericSonarQubeProjectConfiguration(name, ResourceKey.valueOf(key), version);
        }
    }

}
