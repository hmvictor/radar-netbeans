
package qubexplorer;

import static org.hamcrest.CoreMatchers.is;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Victor
 */
public class PassEncoderTest {
    
    @Test
    public void testEncode() {
        String expected="abcde";
        assertThat(PassEncoder.decodeAsString(PassEncoder.encode(expected.toCharArray())), is(expected));
    }
    
}
