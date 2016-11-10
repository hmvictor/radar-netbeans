package qubexplorer;

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
public class ResourceKeyTest {
    private final String first;
    private final String second;
    private final String expected;

    public ResourceKeyTest(String first, String second, String expected) {
        this.first = first;
        this.second = second;
        this.expected = expected;
    }
    
    @Test
    public void shouldBeConcatenated(){
        assertThat(ResourceKey.valueOf(first).concat(ResourceKey.valueOf(second)), is(ResourceKey.valueOf(expected)));
    }

    @Parameterized.Parameters
    public static Object[][] getParameters(){
        return new Object[][]{
            {"a", "b", "a:b"},
            {"a:b", "c", "a:b:c"},
            {"a", "b:c", "a:b:c"}
        };
    }
    
}
