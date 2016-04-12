package qubexplorer.runner;

import java.util.Properties;
import qubexplorer.server.Version;

/**
 *
 * @author Victor
 */
interface VersionConfig {

    boolean applies(Version sonarQubeVersion);

    void apply(SonarRunnerProccess proccess, Properties properties);
    
}
