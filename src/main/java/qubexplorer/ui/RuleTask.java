package qubexplorer.ui;

import org.openide.windows.WindowManager;
import qubexplorer.Rule;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.task.Task;

/**
 *
 * @author Victor
 */
public class RuleTask extends Task<Rule>{
    private final SonarQube sonarQube;
    private final Rule rule;

    public RuleTask(SonarQube sonarQube, Rule rule, ProjectContext projectContext) {
        super(projectContext, sonarQube.getServerUrl());
        this.sonarQube=sonarQube;
        this.rule=rule;
    }

    @Override
    public Rule execute() {
        return sonarQube.getRule(getUserCredentials(), rule.getKey());
    }

    @Override
    protected void success(Rule ruleInServer) {
        rule.setDescription(ruleInServer.getDescription());
        RuleDialog.showRule(WindowManager.getDefault().getMainWindow(), ruleInServer);
    }
    
}
