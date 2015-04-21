import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.*;

public class OptimizerTest {

    @Test
    public void testGeneratePowerset() throws Exception {
        List<String> empty = Collections.emptyList();
        List<String> single = new ArrayList<>();
        single.add("a");
        List<String> pair = new ArrayList<>();
        pair.add("a");
        pair.add("b");
        List<String> triplet = new ArrayList<>();
        triplet.add("a");
        triplet.add("b");
        triplet.add("c");
        List<String> many = new ArrayList<>();
        many.add("a");
        many.add("b");
        many.add("c");
        many.add("d");
        many.add("e");
        assertThat(Optimizer.generatePowerset(empty).size(), is(1));

        assertThat(Optimizer.generatePowerset(single).size(), is(2));
        assertThat(Optimizer.generatePowerset(single).get(0), is(Collections.emptyList()));
        assertThat(Optimizer.generatePowerset(single).get(1), is(Collections.singletonList("a")));

        assertThat(Optimizer.generatePowerset(pair).size(), is(4));
        assertThat(Optimizer.generatePowerset(pair).get(0), is(Collections.emptyList()));
        assertThat(Optimizer.generatePowerset(pair).get(1), is(Collections.singletonList("a")));
        assertThat(Optimizer.generatePowerset(pair).get(2), is(Collections.singletonList("b")));
        assertThat(Optimizer.generatePowerset(pair).get(3), is(Arrays.asList("a", "b")));

        assertThat(Optimizer.generatePowerset(triplet).size(), is(8));
        assertThat(Optimizer.generatePowerset(many).size(), is(32));
    }
}