package qubexplorer.runner;

import java.util.Arrays;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 * @author Victor
 */
@RunWith(Parameterized.class)
public class SonarRunnerProccessTest {

    private final String version;
    private final int expected;

    public SonarRunnerProccessTest(String version, int expected) {
        this.version = version;
        this.expected = expected;
    }

    @Test
    public void testVersion() {
        assertThat(SonarRunnerProccess.getMajorVersion(version), is(expected));
    }

    @Parameterized.Parameters
    public static List<Object[]> getData() {
        return Arrays.asList(new Object[][]{
            {"3.6", 3},
            {"4.0", 4},
            {"4.1", 4},
            {"4.5-RC3", 4}
        });
    }

}
