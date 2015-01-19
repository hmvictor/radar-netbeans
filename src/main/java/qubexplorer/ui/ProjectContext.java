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

//    private final ResourceKey projectKey;
//    public ProjectContext(Project project, ResourceKey projectKey) {
//        this.project = project;
//        this.projectKey = projectKey;
//    }
    public ProjectContext(Project project, SonarQubeProjectConfiguration configuration) {
        this.project = project;
        this.configuration = configuration;
    }
    
    public ProjectContext(Project project) {
        this.project = project;
//        this.projectKey = null;
    }
    
    public Project getProject() {
        return project;
    }

//    public ResourceKey getProjectKey() {
//        return projectKey;
//    }
    public SonarQubeProjectConfiguration getConfiguration() {
        return configuration;
    }
    
}
