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

    private static class CostModel {

        private double r;
        private double t;
        private double l;
        private double m;
        private double a;
        private double f;

        /**
         *
         * @param a the cost of writing an answer to the answer array and incrementing the answer array counter
         * @param f the cost of applying function f to its argument
         * @param l the cost of performing a logical “and”
         * @param m the cost of a branch misprediction
         * @param r the cost of accessing an array element rj[i] in order to perform operations on it
         * @param t the cost of performing an if test
         */
        public CostModel(double a, double f, double l, double m, double r, double t) {
            this.a = a;
            this.f = f;
            this.l = l;
            this.m = m;
            this.r = r;
            this.t = t;
        }
    }

    private final CostModel costModel;
    private final List<Double> selectivities;

    public Optimizer(List<Double> selectivities, Properties costs) {
        this.selectivities = selectivities;
        double r = Double.valueOf(costs.getProperty("r"));
        double t = Double.valueOf(costs.getProperty("t"));
        double l = Double.valueOf(costs.getProperty("l"));
        double m = Double.valueOf(costs.getProperty("m"));
        double a = Double.valueOf(costs.getProperty("a"));
        double f = Double.valueOf(costs.getProperty("f"));
        this.costModel = new CostModel(a, f, l, m, r, t);
    }

    public String process() {
        // 1. Create an array A[] of size 2^k indexed by the subsets of S
        int size = (int) Math.pow(2.0, selectivities.size());
        Set<Record> records = new HashSet<>(size);
        List<Set<Record>> sets = generatePowerset(records); // TODO: should be selectivities instead
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
