package qubexplorer.server;

/**
 *
 * @author Victor
 */
public class Version {
    private final String versionString;
    private final String[] tokens;

    public Version(String versionString) {
        this.versionString = versionString;
        tokens=versionString.split("\\.");
    }
    
    public int getMajor(){
        if (tokens.length >= 1) {
            return Integer.parseInt(tokens[0]);
        } else {
            throw new IllegalArgumentException("Problem getting major version in " + versionString);
        }
    }
    
    public int getMinor(){
        if (tokens.length >= 2) {
            return Integer.parseInt(tokens[1]);
        } else {
            throw new IllegalArgumentException("Problem getting minor version in " + versionString);
        }
    }
    
    public int getTokenCount(){
        return tokens.length;
    }
    
    public String getToken(int index){
        return tokens[index];
    }
    
    @Override
    public String toString(){
        return versionString;
    }
    
    public int compareTo(int major, int minor){
        if(getMajor() > major){
            return 1;
        }
        if(getMajor() < major) {
            return -1;
        }
        String minorToken = getTokenCount() >= 2 ? getToken(1): "0";
        return minorToken.compareTo(String.valueOf(minor));
    }
    
}
