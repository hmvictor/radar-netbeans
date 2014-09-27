package qubexplorer;

import java.util.Objects;

/**
 *
 * @author Victor
 */
public class UserCredentials {
    private final String username;
    private char[] password;

    public UserCredentials(String username, char[] password) {
        Objects.requireNonNull(username, "username is null");
        Objects.requireNonNull(password, "password is null");
        this.username = username;
        this.password = PassEncoder.encode(password);
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        if(password == null) {
            throw new IllegalStateException("password has been cleaned");
        }
        return password.clone();
    }
    
    public void done(){
        for (int i = 0; i < password.length; i++) {
            password[i]=0;
        }
        password=null;
    }
    
}
