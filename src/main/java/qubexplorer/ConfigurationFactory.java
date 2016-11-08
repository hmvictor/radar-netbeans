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
public final class ConfigurationFactory {

    private ConfigurationFactory() {
    }

    public static SonarQubeProjectConfiguration createDefaultConfiguration(Project project) {
        if (SonarMvnProject.isMvnProject(project)) {
            return new SonarMvnProject(project);
        } else {
            return new PlainDirectoryProject(project);
        }
    }

}
