package qubexplorer.ui;

import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import org.openide.util.Exceptions;

/**
 *
 * @author Victor
 */
public abstract class UITask<R, P> extends SwingWorker<R, P> {

    @Override
    protected void done() {
        try {
            R result = get();
            success(result);
        } catch (ExecutionException ex) {
            error(ex.getCause());
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            finished();
        }
    }

    protected void error(Throwable cause) {
        Exceptions.printStackTrace(cause);
    }

    protected void finished() {
        
    }

    protected void success(R result) {
        
    }
    
}
