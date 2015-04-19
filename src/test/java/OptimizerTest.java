import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.*;

public class OptimizerTest {

    @Test
    public void testGeneratePowerset() throws Exception {
        Set<String> empty = Collections.emptySet();
        Set<String> single = new HashSet<>();
        single.add("a");
        Set<String> pair = new HashSet<>();
        pair.add("a");
        pair.add("b");
        Set<String> triplet = new HashSet<>();
        triplet.add("a");
        triplet.add("b");
        triplet.add("c");
        Set<String> many = new HashSet<>();
        many.add("a");
        many.add("b");
        many.add("c");
        many.add("d");
        many.add("e");
        assertThat(Optimizer.generatePowerset(empty).size(), is(1));
        assertThat(Optimizer.generatePowerset(single).size(), is(2));
        assertThat(Optimizer.generatePowerset(pair).size(), is(4));
        assertThat(Optimizer.generatePowerset(triplet).size(), is(8));
        assertThat(Optimizer.generatePowerset(many).size(), is(32));
    }
}