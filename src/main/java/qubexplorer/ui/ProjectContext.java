package qubexplorer.ui;

import org.netbeans.api.project.Project;
import qubexplorer.SonarQubeProjectConfiguration;

/**
 *
 * @author Victor
 */
public class ProjectContext {
    private final Project project;
    private SonarQubeProjectConfiguration configuration;

    public ProjectContext(Project project, SonarQubeProjectConfiguration configuration) {
        this.project = project;
        this.configuration = configuration;
    }
    
    public ProjectContext(Project project) {
        this.project = project;
    }
    
    public Project getProject() {
        return project;
    }

    public SonarQubeProjectConfiguration getConfiguration() {
        return configuration;
    }
    
}
