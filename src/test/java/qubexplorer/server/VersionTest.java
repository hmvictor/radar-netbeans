
package qubexplorer.server;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 * @author Victor
 */
public class VersionTest {
    @Rule
    public ExpectedException ex=ExpectedException.none();
    
    @Test
    public void testWithThreeLevels() {
        Version version = new Version("3.7.1");
        assertThat(version.getMajor(), is(3));
        assertThat(version.getMinor(), is(7));
    }
    
    @Test
    public void testWithTwoLevels() {
        Version version = new Version("3.7");
        assertThat(version.getMajor(), is(3));
        assertThat(version.getMinor(), is(7));
    }
    
    @Test
    public void testWithOneLevel() {
        Version version = new Version("3");
        assertThat(version.getMajor(), is(3));
    }
    
    @Test
    public void testWithRC3() {
        ex.expect(IllegalArgumentException.class);
        Version version = new Version("4.5-RC3");
        assertThat(version.getMajor(), is(4));
        assertThat(version.getMinor(), is(5));
    }
    
    @Test
    public void testCompareVersions() {
        assertThat(new Version("4.6").compareTo(4, 5) >= 0, is(true));
        assertThat(new Version("4.5-RC3").compareTo(4, 5) >= 0, is(true));
        assertThat(new Version("4.5").compareTo(4, 5) >= 0, is(true));
        assertThat(new Version("4.5.1").compareTo(4, 5) >= 0, is(true));
        assertThat(new Version("6.0").compareTo(4, 5) >= 0, is(true));
        assertThat(new Version("6").compareTo(4, 5) >= 0, is(true));
        assertThat(new Version("4.4.1").compareTo(4, 5) >= 0, is(false));
        assertThat(new Version("4.4").compareTo(4, 5) >= 0, is(false));
        assertThat(new Version("4.0").compareTo(4, 5) >= 0, is(false));
        assertThat(new Version("4").compareTo(4, 5) >= 0, is(false));
        assertThat(new Version("3.6").compareTo(4, 5) >= 0, is(false));
        assertThat(new Version("3.5").compareTo(4, 5) >= 0, is(false));
        
        assertThat(new Version("4.5").compareTo(4, 5) == 0, is(true));
        assertThat(new Version("4.5.1").compareTo(4, 5) == 0, is(true));
        assertThat(new Version("4.6").compareTo(4, 5) > 0, is(true));
        assertThat(new Version("4.6.1").compareTo(4, 5) > 0, is(true));
        assertThat(new Version("4.4").compareTo(4, 5) < 0, is(true));
        assertThat(new Version("4.4.1").compareTo(4, 5) < 0, is(true));
    }
    
}
