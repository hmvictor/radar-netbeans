package qubexplorer;

/**
 *
 * @author Victor
 */
public class Authentication {
    private String username;
    private char[] password;

    public Authentication(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password;
    }
    
}
