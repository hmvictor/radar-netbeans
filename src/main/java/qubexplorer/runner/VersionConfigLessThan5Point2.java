package qubexplorer.runner;

import java.util.Properties;
import qubexplorer.server.Version;

/**
 *
 * @author Victor
 */
class VersionConfigLessThan5Point2 implements VersionConfig {
    
    @Override
    public boolean applies(Version sonarQubeVersion) {
        return sonarQubeVersion.getMajor() >= 4 && sonarQubeVersion.compareTo(5, 2) <= 0;
    }

    @Override
    public void apply(SonarRunnerProccess proccess, Properties properties) {
        properties.setProperty("sonar.analysis.mode", proccess.getAnalysisMode().toString().toLowerCase());
    }
    
}
