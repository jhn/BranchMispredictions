import java.util.*;
import java.util.concurrent.Callable;

public class Optimizer implements Callable<String> {

    private static class Record {
        /**
         * Number of terms corresponding to each subset.
         */
        int n;

        /**
         * Product of the selectivities of all terms in the subset.
         */
        double p;

        /**
         * Whether the no-branch optimization was used to get the best cost.
         */
        boolean b;

        /**
         * Current best cost for the subset.
         */
        double c;

        /**
         * Left child of the sub plan.
         */
        Record L;

        /**
         * Right child of the sub plan.
         */
        Record R;
    }

    private final List<Double> selectivities;
    private double r;
    private double t;
    private double l;
    private double m;
    private double a;
    private double f;

    public Optimizer(List<Double> selectivities, Properties costs) {
        this.selectivities = selectivities;
        this.r = Double.valueOf(costs.getProperty("r"));
        this.t = Double.valueOf(costs.getProperty("t"));
        this.l = Double.valueOf(costs.getProperty("l"));
        this.m = Double.valueOf(costs.getProperty("m"));
        this.a = Double.valueOf(costs.getProperty("a"));
        this.f = Double.valueOf(costs.getProperty("f"));
    }

    public String process() {
        // 1. Create an array A[] of size 2^k indexed by the subsets of S
        int size = (int) Math.pow(2.0, selectivities.size()) - 1;
        Set<Record> records = new HashSet<>(size);
        List<Set<Record>> sets = generatePowerset(records);
        return "Process me! :-(";
    }

    @Override
    public String call() throws Exception {
        return process();
    }

    public static <T> List<Set<T>> generatePowerset(Set<T> originalSet) {
        List<Set<T>> sets = new ArrayList<>();
        if (originalSet.isEmpty()) {
            sets.add(new HashSet<>());
            return sets;
        }
        List<T> list = new ArrayList<>(originalSet);
        T head = list.get(0);
        Set<T> rest = new HashSet<>(list.subList(1, list.size()));
        for (Set<T> set : generatePowerset(rest)) {
            Set<T> newSet = new HashSet<>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        return sets;
    }
}
