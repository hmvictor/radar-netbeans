package qubexplorer.runner;

/**
 *
 * @author Victor
 */
public class SonarRunnerException extends RuntimeException{

    public SonarRunnerException() {
    }
    
    public SonarRunnerException(String message) {
        super(message);
    }

    public SonarRunnerException(Throwable thrwbl) {
        super(thrwbl);
    }
    
}
