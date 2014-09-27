package qubexplorer;

import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author Victor
 */
public final class PassEncoder {
    
    private PassEncoder() {
        
    }
    
    public static char[] encode(char[] chars){
        return new String(Base64.encodeBase64(new String(chars).getBytes(StandardCharsets.UTF_8))).toCharArray();
    }
    
    public static char[] decode(char[] chars){
        return decodeAsString(chars).toCharArray();
    }
    
    public static String decodeAsString(char[] chars){
        return new String(Base64.decodeBase64(new String(chars).getBytes()), StandardCharsets.UTF_8);
    }
    
}
