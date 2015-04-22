import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.*;

public class OptimizerTest {

    private static List<String> empty;
    private static List<String> single;
    private static List<String> pair;
    private static List<String> triplet;
    private static List<String> many;

    private static List<Boolean> b1;
    private static List<Boolean> b2;
    private static List<Boolean> b3;
    private static List<Boolean> b4;
    private static List<Boolean> b5;

    @BeforeClass
    public static void setUp() {
        empty =   Collections.emptyList();
        single =  Collections.singletonList("a");
        pair =    Arrays.asList("a", "b");
        triplet = Arrays.asList("a", "b", "c");
        many =    Arrays.asList("a", "b", "c", "d", "e");
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
    public void testBitUnion() throws Exception {
        List<Boolean> b1 = Arrays.asList(true, false, true, false, true, false);
        List<Boolean> b2 = Arrays.asList(false, true, false, true, false, true);
        List<Boolean> b3 = Arrays.asList(true, true, true, true, true, true);
        List<Boolean> b4 = Arrays.asList(false, false, false, false, false, false);
        List<Boolean> b5 = Arrays.asList(true, true, true, false, false, false);

        assertEquals(Optimizer.bitUnion(b1, b2), Arrays.asList(true, true, true, true, true, true));

        assertEquals(Optimizer.bitUnion(b3, b4), Arrays.asList(true, true, true, true, true, true));

        assertEquals(Optimizer.bitUnion(b4, b5), Arrays.asList(true, true, true, false, false, false));

        assertEquals(Optimizer.bitUnion(b4, b4), Arrays.asList(false, false, false, false, false, false));
    }

    @Test
    public void testBitIntersection() throws Exception {
        List<Boolean> b1 = Arrays.asList(true, false, true, false, true, false);
        List<Boolean> b2 = Arrays.asList(false, true, false, true, false, true);
        List<Boolean> b3 = Arrays.asList(true, true, true, true, true, true);
        List<Boolean> b4 = Arrays.asList(false, false, false, false, false, false);
        List<Boolean> b5 = Arrays.asList(true, true, true, false, false, false);

        assertThat(Optimizer.bitIntersect(b1, b2), is(false));
        assertThat(Optimizer.bitIntersect(b2, b1), is(false));

        assertThat(Optimizer.bitIntersect(b3, b4), is(false));
        assertThat(Optimizer.bitIntersect(b4, b3), is(false));

        assertThat(Optimizer.bitIntersect(b3, b5), is(true));
        assertThat(Optimizer.bitIntersect(b1, b5), is(true));

        assertEquals(Optimizer.bitIntersection(b3, b5), Arrays.asList(true, true, true, false, false, false));
        assertEquals(Optimizer.bitIntersection(b2, b5), Arrays.asList(false, true, false, false, false, false));
    }
}