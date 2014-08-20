package qubexplorer.ui;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.windows.WindowManager;
import org.sonar.wsclient.services.Rule;
import qubexplorer.server.SonarQube;

/**
 *
 * @author Victor
 */
public class RuleInfoWorker extends SonarQubeWorker<Rule, Void>{
    private final SonarQube sonarQube;
    private final Rule rule;
    private ProgressHandle handle;
    
    public RuleInfoWorker(SonarQube sonarQube, String projectKey, Rule rule) {
        super(projectKey);
        this.sonarQube=sonarQube;
        this.rule=rule;
        setServerUrl(sonarQube.getServerUrl());
        handle = ProgressHandleFactory.createHandle("Sonar-runner");
        handle.start();
        handle.switchToIndeterminate();
    }
    
    @Override
    protected SonarQubeWorker createCopy() {
        RuleInfoWorker ruleInfoWorker = new RuleInfoWorker(sonarQube, getProjectKey(), rule);
        ruleInfoWorker.setServerUrl(sonarQube.getServerUrl());
        return ruleInfoWorker;
    }

    @Override
    protected Rule doInBackground() throws Exception {
        return sonarQube.getRule(getAuthentication(), rule.getKey());
    }

    @Override
    protected void success(Rule ruleInServer) {
        rule.setDescription(ruleInServer.getDescription());
        RuleDialog.showRule(WindowManager.getDefault().getMainWindow(), rule);
    }
    
    @Override
    protected void finished() {
        handle.finish();
    }
    
}
