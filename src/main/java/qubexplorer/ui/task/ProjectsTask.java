package qubexplorer.ui.task;

import java.util.List;
import qubexplorer.server.SonarProject;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.ProjectContext;

/**
 *
 * @author Victor
 */
public class ProjectsTask extends Task<List<SonarProject>>{
    private final SonarQube sonarQube;

    public ProjectsTask(SonarQube sonarQube, ProjectContext projectContext) {
        super(projectContext, sonarQube.getServerUrl());
        this.sonarQube=sonarQube;
    }

    @Override
    public List<SonarProject> execute() throws Exception {
        return sonarQube.getProjects(getToken());
    }
    
}
