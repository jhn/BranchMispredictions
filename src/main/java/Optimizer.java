import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Optimizer implements Callable<String> {

    private static class SubSet {

        /**
         * selectivities: The list of selectivities for this subset.
         * k: Number of terms corresponding to each subset.
         * p: Product of the selectivities of all terms in the subset.
         * b: Whether the no-branch optimization was used to get the best cost.
         * c: Current best cost for the subset.
         * L: Left child of the sub plan.
         * R: Right child of the sub plan.
         */

        List<Double> selectivities;
        int k;
        double p;
        boolean b;
        double c;
        SubSet L;
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

        public double branchingAndCost(SubSet two) {
            double q = p <= 0.5 ? p : 1.0 - p;
            return this.fixedCost() + costModel.m * q + p * two.c;
        }

        public boolean ifLemma48(SubSet two) {
            return two.p <= p && ((two.p - 1.0) / two.fixedCost()) < ((p - 1.0)/ this.fixedCost());
        }

        public boolean ifLemma49(SubSet two) {
            return p <= 0.5 && two.p < p && two.fixedCost() < this.fixedCost();
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
        List<SubSet> subSets = generateSubSets(selectivities, costModel);

        initialCosts(subSets);
        System.out.println(subSets.size());
        subSets.stream().forEachOrdered(s -> {
            subSets.stream().forEachOrdered(sPrime -> {
                List<Double> intersection = intersection(s.selectivities, sPrime.selectivities);
                if (!intersection.isEmpty()) {
                    if (s.ifLemma48(sPrime) || s.ifLemma49(sPrime)) {

                    } else {
                        double c = s.branchingAndCost(sPrime);
                        List<Double> union = union(sPrime.selectivities, s.selectivities);
                        for (SubSet subSet : subSets) {
                            if (subSet.selectivities.equals(union)) {
                                if (c < subSet.c) {
                                    subSet.c = c;
                                    subSet.L = sPrime;
                                    subSet.R = s;
                                }
                                break;
                            }
                        }
                    }
                }
            });
        });
        printOptimalPlan(selectivities, subSets);
        return "Process me! :-(";
    }

    // (E1) && [ (E2) && [ ··· [(En-1) && (En)] ··· ]]
    private static void printOptimalPlan(List<Double> selectivities, List<SubSet> subset) {
        SubSet subSet = subset.get(subset.size() - 1);
        System.out.print("Plan: ");
        selectivities.stream().forEach(s -> System.out.print(s + " "));
        System.out.println();
        double cost = subSet.c;
        System.out.println("Cost: " + cost);
        System.out.println();
    }

    private static List<SubSet> generateSubSets(List<Double> selectivities, CostModel costModel) {
        int subSetSize = (int) (Math.pow(2.0, (double) selectivities.size()) - 1);
        List<List<Double>> selectivityPowerSet = powerSet(selectivities);
        List<SubSet> subSets = new ArrayList<>(subSetSize);
        for (List<Double> selectivityList : selectivityPowerSet) {
            SubSet subSet = new SubSet();
            subSet.k = selectivityList.size();
            subSet.selectivities = selectivityList;
            subSet.p = subSet.selectivities.stream().reduce(1.0, (a, b) -> a * b);
            subSet.costModel = costModel;

            subSets.add(subSet);
        }
        return subSets;
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

    public static <T> List<List<T>> powerSet(List<T> originalSet) {
        List<List<T>> sets = new ArrayList<>();
        if (originalSet.isEmpty()) {
            sets.add(new ArrayList<>());
            return sets;
        }
        List<T> list = new ArrayList<>(originalSet);
        T head = list.get(0);
        List<T> rest = new ArrayList<>(list.subList(1, list.size()));
        for (List<T> set : powerSet(rest)) {
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

    public static <T> List<T> union(List<T> first, List<T> second) {
        return Stream.concat(first.stream(), second.stream()).distinct().collect(Collectors.toList());
    }

    public static <T> List<T> intersection(List<T> first, List<T> second) {
        return first.stream().filter(second::contains).collect(Collectors.toList());
    }
}
