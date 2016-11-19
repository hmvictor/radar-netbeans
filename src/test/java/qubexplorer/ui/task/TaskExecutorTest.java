package qubexplorer.ui.task;

import java.util.concurrent.CountDownLatch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import qubexplorer.UserCredentials;
import qubexplorer.AuthorizationException;
import qubexplorer.GenericSonarQubeProjectConfiguration;
import qubexplorer.ResourceKey;
import qubexplorer.ui.UserCredentialsRepository;
import qubexplorer.ui.ProjectContext;

/**
 *
 * @author Victor
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskExecutorTest {

    @Spy
    private TaskImpl<Boolean> task = new TaskImpl<>(new ProjectContext(null, new GenericSonarQubeProjectConfiguration("name", ResourceKey.valueOf("part1:part2"), "1.0")), "http://testhost:9000");

    @Mock
    private UserCredentialsRepository repository;

    @Before
    public void init() {
        when(repository.getUserCredentials(anyString(), (ResourceKey) anyObject())).thenReturn(new UserCredentials("username", new char[0]));
    }

    @Test(timeout = 5000)
    public void shouldCallInit() throws Exception {
        when(task.execute()).thenReturn(Boolean.TRUE);
        TaskExecutor.execute(repository, task);
        task.waitForDestruction();
        verify(task).init();
    }

    @Test(timeout = 5000)
    public void shouldExecute() throws Exception {
        when(task.execute()).thenReturn(Boolean.TRUE);
        TaskExecutor.execute(repository, task);
        task.waitForDestruction();
        verify(task).execute();
    }

    @Test(timeout = 5000)
    public void shouldCallCompleted() throws Exception {
        when(task.execute()).thenReturn(Boolean.TRUE);
        TaskExecutor.execute(repository, task);
        task.waitForDestruction();
        verify(task).completed();
    }

    @Test(timeout = 5000)
    public void shouldCallSuccess() throws Exception {
        when(task.execute()).thenReturn(Boolean.TRUE);
        TaskExecutor.execute(repository, task);
        task.waitForDestruction();
        verify(task).success(Boolean.TRUE);
    }

    @Test(timeout = 5000)
    public void shouldCallFail() throws Exception {
        when(task.execute()).thenThrow(new IllegalArgumentException());
        TaskExecutor.execute(repository, task);
        task.waitForDestruction();
        verify(task).fail(isA(IllegalArgumentException.class));
    }

    @Test
    public void shouldCallReset() throws Exception {
        when(task.execute())
                .thenThrow(new AuthorizationException())
                .thenReturn(Boolean.TRUE);
        TaskExecutor.execute(repository, task);
        task.waitForDestruction();
        verify(task).reset();
    }

    @Test(timeout = 5000)
    public void shouldCallSuccessAfterAuthFailed() throws Exception {
        when(task.execute())
                .thenThrow(new AuthorizationException())
                .thenReturn(Boolean.TRUE);
        TaskExecutor.execute(repository, task);
        task.waitForDestruction();
        verify(task).success(Boolean.TRUE);
    }

    @Test(timeout = 5000)
    public void shouldCallDestroy() throws Exception {
        when(task.execute()).thenReturn(Boolean.TRUE);
        TaskExecutor.execute(repository, task);
        task.waitForDestruction();
        verify(task).destroy();
    }

    public static class TaskImpl<T> extends Task<T> {
        private boolean destroyed = false;
        private final CountDownLatch lock=new CountDownLatch(1);

        public TaskImpl(ProjectContext projectContext, String serverUrl) {
            super(projectContext, serverUrl);
        }

        @Override
        protected void destroy() {
            destroyed = true;
            lock.countDown();
        }

        private void waitForDestruction(){
            while(!destroyed){
                try {
                    lock.await();
                } catch (InterruptedException ex) {
                    
                }
            }
        }

        @Override
        public T execute() throws TaskExecutionException {
            return null;
        }

    }

}
