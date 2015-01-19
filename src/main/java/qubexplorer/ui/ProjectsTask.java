package qubexplorer.ui;

import java.util.List;
import qubexplorer.SonarQubeProjectConfiguration;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.task.Task;

/**
 *
 * @author Victor
 */
public class ProjectsTask extends Task<List<SonarQubeProjectConfiguration>>{
    private final SonarQube sonarQube;

    public ProjectsTask(SonarQube sonarQube, ProjectContext projectContext) {
        super(projectContext, sonarQube.getServerUrl());
        this.sonarQube=sonarQube;
    }

    @Override
    public List<SonarQubeProjectConfiguration> execute() throws Exception {
        return sonarQube.getProjects(getUserCredentials());
    }
    
}
