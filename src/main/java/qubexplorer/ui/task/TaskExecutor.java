package qubexplorer.ui.task;

import java.util.concurrent.ExecutionException;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;
import qubexplorer.UserCredentials;
import qubexplorer.AuthorizationException;
import qubexplorer.ui.AuthDialog;
import qubexplorer.ui.AuthenticationRepository;

/**
 *
 * @author Victor
 */
public final class TaskExecutor {
    
    private TaskExecutor() {
        
    }

    public static <T> void execute(Task<T> task) {
        execute(AuthenticationRepository.getInstance(), task);
    }

    public static <T> void execute(final AuthenticationRepository repository, final Task<T> task) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                new TaskWorker<>(repository, task).execute();
            }

        });

    }

    private static class TaskWorker<T> extends SwingWorker<T, Void> {

        private final AuthenticationRepository authenticationRepository;
        private final Task<T> task;
        private ProgressHandle handle;
        //persistent mode

        public TaskWorker(AuthenticationRepository repository, Task<T> task) {
            this.authenticationRepository = repository;
            this.task = task;
            assert SwingUtilities.isEventDispatchThread();
            handle = ProgressHandleFactory.createHandle("Sonar");
            handle.start();
            handle.switchToIndeterminate();
            this.task.init();
        }

        @Override
        protected final T doInBackground() throws Exception {
            return task.execute();
        }

        @Override
        protected final void done() {
            boolean willRetry = false;
            try {
                T result = get();
                task.completed();
                task.success(result);
                handle.finish();
                handle = null;
                if (task.getUserCredentials() != null) {
                    assert task.getServerUrl() != null;
                    authenticationRepository.saveAuthentication(task.getServerUrl(), task.getProjectContext().getConfiguration().getKey().toString(), task.getUserCredentials());
                }
            } catch (ExecutionException ex) {
                handle.finish();
                handle = null;
                Throwable cause = ex.getCause();
                if (cause instanceof AuthorizationException) {
                    assert task.getServerUrl() != null;
                    String resourceKey = null;
                    if (task.getProjectContext() != null) {
                        resourceKey = task.getProjectContext().getConfiguration().getKey().toString();
                    }
                    UserCredentials auth = authenticationRepository.getAuthentication(task.getServerUrl(), resourceKey);
                    if (auth == null) {
                        auth = AuthDialog.showAuthDialog(WindowManager.getDefault().getMainWindow());
                    }
                    if (auth != null) {
                        willRetry = true;
                        task.reset();
                        task.setUserCredentials(auth);
                        TaskExecutor.execute(authenticationRepository, task);
                    }
                } else {
                    task.completed();
                    task.fail(cause);
                }
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                if (!willRetry) {
                    task.destroy();
                }
                if (handle != null) {
                    handle.finish();
                }
            }
        }

    }

}
