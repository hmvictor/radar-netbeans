package qubexplorer;

/**
 *
 * @author Victor
 */
public class AuthenticationToken {
    private String username;
    private char[] password;

    public AuthenticationToken(String username, char[] password) {
        this.username = username;
        this.password = password.clone();
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password.clone();
    }
    
}
