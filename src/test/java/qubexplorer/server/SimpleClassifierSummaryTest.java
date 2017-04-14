package qubexplorer.server;

import java.util.Objects;
import javax.swing.Icon;
import static org.hamcrest.CoreMatchers.is;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import qubexplorer.Classifier;
import qubexplorer.Rule;
import qubexplorer.filter.IssueFilter;

/**
 *
 * @author Víctor
 */
public class SimpleClassifierSummaryTest {
    
    private SimpleClassifierSummary<MyClassifier> summary;
    private MyClassifier firstClassifier;
    private MyClassifier secondClassifier;
    
    @Before
    public void init() {
        firstClassifier = new MyClassifier("first");
        secondClassifier = new MyClassifier("second");
        summary= new SimpleClassifierSummary<>();
    }

    @Test
    public void shouldHaveCountByClassifier() {
        summary.increment(firstClassifier, new Rule("a"), 1);
        summary.increment(firstClassifier, new Rule("b"), 2);
        summary.increment(firstClassifier, new Rule("c"), 3);
        assertThat(summary.getCount(firstClassifier), is(6));
        assertThat(summary.getCount(secondClassifier), is(0));
    }

    @Test
    public void shouldHaveCountByRule() {
        Rule first=new Rule("first");
        Rule second=new Rule("second");
        summary.increment(firstClassifier, first, 1);
        summary.increment(firstClassifier, first, 2);
        summary.increment(firstClassifier, first, 3);
        assertThat(summary.getCount(first), is(6));
        assertThat(summary.getCount(second), is(0));
    }
    
    @Test
    public void shouldHaveTotalCount() {
        assertThat(summary.getCount(), is(0));
        summary.increment(new MyClassifier("a"), new Rule("a"), 1);
        summary.increment(new MyClassifier("b"), new Rule("b"), 2);
        summary.increment(new MyClassifier("c"), new Rule("c"), 3);
        summary.increment(new MyClassifier("d"), new Rule("d"), 4);
        assertThat(summary.getCount(), is(10));
    }

    public static class MyClassifier implements Classifier {
        private final String identifier;

        public MyClassifier(String identifier) {
            this.identifier = identifier;
        }
        
        @Override
        public IssueFilter createFilter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + Objects.hashCode(this.identifier);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MyClassifier other = (MyClassifier) obj;
            if (!Objects.equals(this.identifier, other.identifier)) {
                return false;
            }
            return true;
        }

        @Override
        public Icon getIcon() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getUserDescription() {
            throw new UnsupportedOperationException();
        }

    }

}
