import jdk.nashorn.internal.runtime.ECMAException;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Optimizer implements Callable<String> {

    private static class SubSet {

        /**
         * values: The list of values for this subset.
         * k: Number of terms corresponding to each subset.
         * p: Product of the values of all terms in the subset.
         * b: Whether the no-branch optimization was used to get the best cost.
         * c: Current best cost for the subset.
         * L: Left child of the sub plan.
         * R: Right child of the sub plan.
         */

        Selectivities selectivities;
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

        public static double combinedCost(SubSet one, SubSet two) {
            double q = Math.min(one.p, 1.0 - one.p);
            return one.fixedCost() + one.costModel.m * q + one.p * two.c;
        }

        public boolean lemma48(SubSet two) {
            return two.p <= p && ((two.p - 1.0) / two.fixedCost()) < ((p - 1.0) / this.fixedCost());
        }

        public boolean lemma49(SubSet two) {
            return two.p < p && two.fixedCost() < this.fixedCost();
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

        initializeCosts(subSets);

        for (SubSet s : subSets) {
            for (SubSet sPrime : subSets) {
                if (!bitIntersect(s.selectivities.bits, sPrime.selectivities.bits)) {
                    if (s.lemma48(sPrime)) {
                        continue;
                    } else if (sPrime.p <= 0.5 && s.lemma49(sPrime)) {
                        continue;
                    } else {
                        double c = SubSet.combinedCost(sPrime, s);
                        List<Boolean> union = bitUnion(sPrime.selectivities.bits, s.selectivities.bits);
                        SubSet subset = findSubSetByBits(subSets, union);
                        if (c < subset.c) {
                            subset.c = c;
                            subset.L = sPrime;
                            subset.R = s;
                        }
                    }
                }
            }
        }
        printOptimalPlan(selectivities, subSets);
        return "Process me! :-(";
    }

    private static SubSet findSubSetByBits(List<SubSet> globalList, List<Boolean> bits) {
        return globalList
                .stream()
                .filter(ss -> ss.selectivities.bits.equals(bits))
                .findFirst().get();
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
        int subSetSize = (int) (Math.pow(2.0, selectivities.size()) - 1);

        // gets a list initialized to (0, 1, 2... selectivities.size - 1)

        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < selectivities.size(); i++) {
             numbers.add(i);
        }
        // generates the powerset for the list
        List<List<Integer>> positions = powerSet(numbers);
        positions.remove(0); // get rid of the empty set
        // for each one of these lists of lists
        List<List<Boolean>> bitSetsPowerSets = new ArrayList<>(positions.size());
        for (List<Integer> position : positions) {
            bitSetsPowerSets.add(bitSetFromPositions(position, selectivities.size()));
        }

        List<SubSet> subSets = new ArrayList<>(subSetSize);
        for (List<Boolean> bitSet : bitSetsPowerSets) {
            SubSet subSet = new SubSet();
            subSet.k = Collections.frequency(bitSet, true);
            subSet.selectivities = new Selectivities(bitSet, selectivities);
            subSet.p = subSet.selectivities.calculateP();
            subSet.costModel = costModel;

            subSets.add(subSet);
        }
        return subSets;
    }

    private static List<Boolean> bitSetFromPositions(List<Integer> positions, int length) {
        Boolean bitSet[] = new Boolean[length];
        for (int i = 0; i < length; i++) {
            bitSet[i] = false;
        }
        for (int i = 0; i < positions.size(); i++) {
            bitSet[positions.get(i)] = true;
        }
        return Arrays.asList(bitSet);
    }

    private static void initializeCosts(List<SubSet> subSets) {
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

    public static class Selectivities {
        List<Boolean> bits;         // some subset of the values
        List<Double> values;        // The global set of selectivities

        public Selectivities(List<Boolean> bits, List<Double> values) {
            this.bits = bits;
            this.values = values;
        }

        /**
         * Gets bits turned on at the bit locations.
         * @return
         */
        public List<Double> getSelectivitiesFromBits() {
            Double result[] = new Double[this.values.size()];
            for (int i = 0; i < this.values.size(); i++) {
                result[i] = 0.0;
            }
            for (int i = 0; i < this.bits.size(); i++) {
                if (this.bits.get(i)) {
                    result[i] = this.values.get(i);
                }
            }
            return Arrays.asList(result);
        }

        /**
         * Gets the product of the selectivities specified by the bit list.
         * @return
         */
        public double calculateP() {
            return getSelectivitiesFromBits()
                    .stream()
                    .filter(s -> s != 0.0)
                    .reduce(1.0, (a, b) -> a * b);
        }
    }

    public static <T> List<List<T>> order(List<List<T>> list) {
        Collections.sort(list, (o1, o2) -> o1.size() - o2.size());
        return list;
    }

    public static List<Boolean> bitUnion(List<Boolean> first, List<Boolean> second) {
        List<Boolean> union = new ArrayList<>(first.size());
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i) || second.get(i)) {
                union.add(i, true);
            } else {
                union.add(i, false);
            }
        }
        return union;
    }

    public static List<Boolean> bitIntersection(List<Boolean> first, List<Boolean> second) {
        List<Boolean> intersection = new ArrayList<>(first.size());
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i) && second.get(i)) {
                intersection.add(i, true);
            } else {
                intersection.add(i, false);
            }
        }
        return intersection;
    }

    /**
     *
     * @param first
     * @param second
     * @return true if there is an intersection between two bit sets, false otherwise
     */
    public static boolean bitIntersect(List<Boolean> first, List<Boolean> second) {
        List<Boolean> booleans = bitIntersection(first, second);
        for (Boolean b : booleans) {
            if (b) {
                return true;
            }
        }
        return false;
    }
}
