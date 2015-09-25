package qubexplorer.ui;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import org.openide.util.Exceptions;

/**
 *
 * @author Victor
 */
public abstract class UITask<R, P> extends SwingWorker<R, P> {
    private static final Logger LOGGER = Logger.getLogger(UITask.class.getName());

    @Override
    protected void done() {
        try {
            R result = get();
            success(result);
        } catch (ExecutionException ex) {
            /* Call template method error handler. */
            LOGGER.log(Level.INFO, ex.getMessage(), ex);
            error(ex.getCause());
        } catch (InterruptedException ex) {
            /* Not an application exception. Local handling of this. */
            LOGGER.log(Level.INFO, ex.getMessage());
            Exceptions.printStackTrace(ex);
        } finally {
            finished();
        }
    }

    /**
     * Called when an error happens.
     * 
     * @param cause 
     */
    protected void error(Throwable cause) {
        Exceptions.printStackTrace(cause);
    }

    /**
     * Called when the task is finished.
     */
    protected void finished() {
        
    }

    /**
     * Called after successful execution.
     * 
     * @param result The result of the execution.
     */
    protected void success(R result) {
        
    }
    
}
