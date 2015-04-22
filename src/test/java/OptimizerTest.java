import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.*;

public class OptimizerTest {

    private static List<String> empty;
    private static List<String> single;
    private static List<String> single2;
    private static List<String> pair;
    private static List<String> triplet;
    private static List<String> many;
    private static List<String> many2;

    @BeforeClass
    public static void setUp() {
        empty =   Collections.emptyList();
        single =  Collections.singletonList("a");
        single2 = Collections.singletonList("z");
        pair =    Arrays.asList("a", "b");
        triplet = Arrays.asList("a", "b", "c");
        many =    Arrays.asList("a", "b", "c", "d", "e");
        many2 =   Arrays.asList("0", "1", "2", "3", "4");
    }

    @Test
    public void testGeneratePowerset() throws Exception {
        assertThat(Optimizer.powerSet(empty).size(), is(1));

        assertThat(Optimizer.powerSet(single).size(), is(2));
        assertThat(Optimizer.powerSet(single).get(0), is(Collections.emptyList()));
        assertThat(Optimizer.powerSet(single).get(1), is(Collections.singletonList("a")));

        assertThat(Optimizer.powerSet(pair).size(), is(4));
        assertThat(Optimizer.powerSet(pair).get(0), is(Collections.emptyList()));
        assertThat(Optimizer.powerSet(pair).get(1), is(Collections.singletonList("a")));
        assertThat(Optimizer.powerSet(pair).get(2), is(Collections.singletonList("b")));
        assertThat(Optimizer.powerSet(pair).get(3), is(Arrays.asList("a", "b")));

        assertThat(Optimizer.powerSet(triplet).size(), is(8));
        assertThat(Optimizer.powerSet(many).size(), is(32));
    }

    @Test
    public void testUnion() throws Exception {
        assertThat(Optimizer.union(empty, single).size(), is(1));
        assertThat(Optimizer.union(single, pair).size(), is(2));
        assertThat(Optimizer.union(single, single2).size(), is(2));
        assertThat(Optimizer.union(single2, triplet).size(), is(4));
    }

    @Test
    public void testIntersection() throws Exception {
        assertThat(Optimizer.intersection(empty, single).size(), is(0));
        assertThat(Optimizer.intersection(single, pair).size(), is(1));
        assertThat(Optimizer.intersection(single, many).size(), is(1));
        assertThat(Optimizer.intersection(pair, many).size(), is(2));
        assertThat(Optimizer.intersection(triplet, many).size(), is(3));
        assertThat(Optimizer.intersection(many, many2).size(), is(0));
    }
}