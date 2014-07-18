
package qubexplorer.ui;

import org.junit.Test;
import static org.junit.Assert.*;
import qubexplorer.AuthenticationToken;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author Victor
 */
public class AuthenticationRepositoryTest {
    
    @Test
    public void shouldContaintAuthentication(){
        AuthenticationRepository repo=AuthenticationRepository.getInstance();
        AuthenticationToken auth=new AuthenticationToken("one", "two".toCharArray());
        repo.saveAuthentication("url", "key", auth);
        assertThat(repo.getAuthentication("url", "key"), is(auth));
        assertThat(repo.getAuthentication("url", "key2"), is(auth));
        AuthenticationToken auth2=new AuthenticationToken("one", "two".toCharArray());
        repo.saveAuthentication("url2", null, auth2);
        assertThat(repo.getAuthentication("url2", "key"), is(auth2));
        assertThat(repo.getAuthentication("url2", "key2"), is(auth2));
        assertNull(repo.getAuthentication("url3", "key2"));
    }
    
}
