package qubexplorer.ui;

import org.netbeans.api.project.Project;

/**
 *
 * @author Victor
 */
public class ProjectContext {
    private final Project project;
    private final String projectKey;

    public ProjectContext(Project project, String projectKey) {
        this.project = project;
        this.projectKey = projectKey;
    }

    public ProjectContext(Project project) {
        this.project = project;
        this.projectKey = null;
    }
    
    public Project getProject() {
        return project;
    }

    public String getProjectKey() {
        return projectKey;
    }
    
}
