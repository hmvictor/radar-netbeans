
package qubexplorer.ui;

import org.junit.Test;
import static org.junit.Assert.*;
import qubexplorer.UserCredentials;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author Victor
 */
public class AuthenticationRepositoryTest {
    
    @Test
    public void shouldContaintAuthentication(){
        AuthenticationRepository repo=AuthenticationRepository.getInstance();
        UserCredentials auth=new UserCredentials("one", "two".toCharArray());
        repo.saveAuthentication("url", "key", auth);
        assertThat(repo.getAuthentication("url", "key"), is(auth));
        assertThat(repo.getAuthentication("url", "key2"), is(auth));
        UserCredentials auth2=new UserCredentials("one", "two".toCharArray());
        repo.saveAuthentication("url2", null, auth2);
        assertThat(repo.getAuthentication("url2", "key"), is(auth2));
        assertThat(repo.getAuthentication("url2", "key2"), is(auth2));
        assertNull(repo.getAuthentication("url3", "key2"));
    }
    
}
