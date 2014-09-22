package qubexplorer;

/**
 *
 * @author Victor
 */
public class UserCredentials {
    private String username;
    private char[] password;

    public UserCredentials(String username, char[] password) {
        this.username = username;
        this.password = password.clone();
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password.clone();
    }
    
    public void done(){
        for (int i = 0; i < password.length; i++) {
            password[i]=0;
        }
    }
    
}
