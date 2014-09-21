package qubexplorer.ui.task;

import org.openide.windows.WindowManager;
import org.sonar.wsclient.services.Rule;
import qubexplorer.server.SonarQube;
import qubexplorer.ui.ProjectContext;
import qubexplorer.ui.RuleDialog;

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
        return sonarQube.getRule(getToken(), rule.getKey());
    }

    @Override
    protected void success(Rule ruleInServer) {
        rule.setDescription(ruleInServer.getDescription());
        RuleDialog.showRule(WindowManager.getDefault().getMainWindow(), ruleInServer);
    }
    
}
