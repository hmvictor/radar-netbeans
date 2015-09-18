package qubexplorer.ui.task;

/**
 * Indicates an error executing a task.
 * 
 * @author Victor
 */
public class TaskExecutionException extends Exception{

    public TaskExecutionException(Throwable cause) {
        super(cause);
    }

}
