
package qubexplorer.ui;

import org.junit.Test;
import static org.junit.Assert.*;
import qubexplorer.UserCredentials;
import static org.hamcrest.CoreMatchers.*;
import qubexplorer.ResourceKey;

/**
 *
 * @author Victor
 */
public class AuthenticationRepositoryTest {
    
    @Test
    public void shouldContaintAuthentication(){
        UserCredentialsRepository repo=UserCredentialsRepository.getInstance();
        UserCredentials auth=new UserCredentials("one", "two".toCharArray());
        repo.saveUserCredentials("url", ResourceKey.valueOf("key"), auth);
        assertThat(repo.getUserCredentials("url", ResourceKey.valueOf("key")), is(auth));
        assertThat(repo.getUserCredentials("url", ResourceKey.valueOf("key2")), is(auth));
        UserCredentials auth2=new UserCredentials("one", "two".toCharArray());
        repo.saveUserCredentials("url2", null, auth2);
        assertThat(repo.getUserCredentials("url2", ResourceKey.valueOf("key")), is(auth2));
        assertThat(repo.getUserCredentials("url2", ResourceKey.valueOf("key2")), is(auth2));
        assertNull(repo.getUserCredentials("url3", ResourceKey.valueOf("key2")));
    }
    
}
