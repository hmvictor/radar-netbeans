package qubexplorer;

import org.netbeans.api.project.Project;

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
