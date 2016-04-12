package qubexplorer.runner;

import java.util.Properties;
import qubexplorer.server.Version;

/**
 *
 * @author Victor
 */
class VersionConfigLessThan4 implements VersionConfig {
    
    @Override
    public boolean applies(Version sonarQubeVersion) {
        return sonarQubeVersion.getMajor() < 4;
    }

    @Override
    public void apply(SonarRunnerProccess proccess, Properties properties) {
        properties.setProperty("sonar.dryRun", "true");
    }
    
}
