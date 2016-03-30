package qubexplorer.runner;

import java.util.Properties;
import qubexplorer.server.Version;

/**
 *
 * @author Victor
 */
class VersionConfigMoreThan5Point2 implements VersionConfig {
    
    @Override
    public boolean applies(Version sonarQubeVersion) {
        return sonarQubeVersion.compareTo(5, 2) >= 0;
    }

    @Override
    public void apply(SonarRunnerProccess proccess, Properties properties) {
        properties.setProperty("sonar.analysis.mode", "issues");
        properties.setProperty("sonar.report.export.path", SonarRunnerProccess.JSON_FILENAME);
    }
    
}
