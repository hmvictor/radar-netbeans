package qubexplorer;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;

/**
 *
 * @author Victor
 */
public class SimpleSonarProject implements SonarQubeProjectInfo {
    private final Project project;

    public SimpleSonarProject(Project project) {
        this.project = project;
    }


    @Override
    public String getName() {
        return ProjectUtils.getInformation(project).getName();
    }

    @Override
    public ResourceKey getKey() {
        return new ResourceKey("base", getName());
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public SonarQubeProjectInfo createSubprojectInfo(Project project) {
        return new SimpleSonarProject(project);
    }
    
}
