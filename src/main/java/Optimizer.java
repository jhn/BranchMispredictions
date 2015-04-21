import java.util.*;
import java.util.concurrent.Callable;

public class Optimizer implements Callable<String> {

    private static class SubSet {

        /**
         * The list of selectivities for this subset.
         */
        List<Double> selectivities;

        /**
         * Represents which selectivities we are currently using.
         */
        BitSet bs;

        /**
         * Number of terms corresponding to each subset.
         */
        int k;

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
        SubSet L;

        /**
         * Right child of the sub plan.
         */
        SubSet R;

        CostModel costModel;

        public double noBranchCost() {
            return k * costModel.r + (k - 1) * costModel.l + costModel.f * k + costModel.a;
        }

        public double logicalAndCost() {
            double q = p <= 0.5 ? p : 1.0 - p;
            return k * costModel.r + (k - 1) * costModel.l + k * costModel.f + costModel.t + costModel.m * q + p * costModel.a;
        }

        public double fixedCost() {
            return k * costModel.r + (k - 1) * costModel.l + k * costModel.f + costModel.t;
        }
    }

    private static class CostModel {

        double r;
        double t;
        double l;
        double m;
        double a;
        double f;

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

    @Override
    public String call() throws Exception {
        // 1. Create an array A[] of size 2^k indexed by the subsets of S
        List<SubSet> subSets = generateSubSets(selectivities, costModel);
        initialCosts(subSets);
        setupP(subSets);
        return "Process me! :-(";
    }

    private static List<SubSet> generateSubSets(List<Double> selectivities, CostModel costModel) {
        int subSetSize = (int) (Math.pow(2.0, (double) selectivities.size()) - 1);
        List<List<Double>> selectivityPowerSet = generatePowerset(selectivities);
        List<SubSet> subSets = new ArrayList<>(subSetSize);
        for (List<Double> selectivityList : selectivityPowerSet) {
            SubSet subSet = new SubSet();
            subSet.k = selectivityList.size();
            subSet.p = 0;
            subSet.costModel = costModel;
            subSet.selectivities = selectivityList;
            subSets.add(subSet);
        }
        return subSets;
    }

    public static <T> List<List<T>> generatePowerset(List<T> originalSet) {
        List<List<T>> sets = new ArrayList<>();
        if (originalSet.isEmpty()) {
            sets.add(new ArrayList<>());
            return sets;
        }
        List<T> list = new ArrayList<>(originalSet);
        T head = list.get(0);
        List<T> rest = new ArrayList<>(list.subList(1, list.size()));
        for (List<T> set : generatePowerset(rest)) {
            List<T> newSet = new ArrayList<>();
            newSet.add(head);
            newSet.addAll(set);
            sets.add(newSet);
            sets.add(set);
        }
        order(sets);
        return sets;
    }

    public static <T> List<List<T>> order(List<List<T>> list) {
        Collections.sort(list, (o1, o2) -> o1.size() - o2.size());
        return list;
    }

    private static void initialCosts(List<SubSet> subSets) {
        for (SubSet subSet : subSets) {
            double logicalAndCost = subSet.logicalAndCost();
            double noBranchCost = subSet.noBranchCost();
            if (noBranchCost < logicalAndCost) {
                subSet.c = noBranchCost;
                subSet.b = true;
            } else {
                subSet.c = logicalAndCost;
            }
        }
    }

    private static void setupP(List<SubSet> subsets) {
        subsets.stream().forEach(subset -> subset.p = subset.selectivities.stream().reduce(1.0, (a, b) -> a * b));
    }
}
