package qubexplorer.ui.task;

import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;
import qubexplorer.UserCredentials;
import qubexplorer.AuthorizationException;
import qubexplorer.ResourceKey;
import qubexplorer.ui.AuthDialog;
import qubexplorer.ui.UserCredentialsRepository;

/**
 *
 * @author Victor
 */
public final class TaskExecutor {

    private TaskExecutor() {

    }

    public static <T> void execute(Task<T> task) {
        execute(UserCredentialsRepository.getInstance(), task);
    }

    public static <T> void execute(final UserCredentialsRepository repository, final Task<T> task) {
        SwingUtilities.invokeLater(() -> {
            new TaskWorker<>(repository, task).execute();
        });
    }

    public static ResourceKey getResourceKey(Task task) {
        if (task.getProjectContext() != null && task.getProjectContext().getConfiguration() != null) {
            return task.getProjectContext().getConfiguration().getKey();
        }
        return null;
    }

    private static class TaskWorker<T> extends SwingWorker<T, Void> {

        private static final Logger LOGGER = Logger.getLogger(TaskWorker.class.getName());

        private final UserCredentialsRepository userCredentialsRepository;
        private final Task<T> task;
        private ProgressHandle handle;

        public TaskWorker(UserCredentialsRepository repository, Task<T> task) {
            this.userCredentialsRepository = repository;
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
                    userCredentialsRepository.saveUserCredentials(task.getServerUrl(), /* XXX There was a null here */ getResourceKey(task), task.getUserCredentials());
                }
            } catch (ExecutionException ex) {
                LOGGER.log(Level.INFO, ex.getMessage(), ex);
                handle.finish();
                handle = null;
                Throwable cause = ex.getCause();
                if (cause instanceof AuthorizationException && task.isRetryIfNoAuthorization()) {
                    assert task.getServerUrl() != null;

                    UserCredentials userCredentials = userCredentialsRepository.getUserCredentials(task.getServerUrl(), getResourceKey(task));
                    if (userCredentials == null) {
                        userCredentials = AuthDialog.showAuthDialog(WindowManager.getDefault().getMainWindow());
                    }

                    if (userCredentials != null) {
                        willRetry = true;
                        retryTask(userCredentials);
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

        private void retryTask(UserCredentials userCredentials) {
            task.reset();
            task.setUserCredentials(userCredentials);
            TaskExecutor.execute(userCredentialsRepository, task);
        }

    }

}
