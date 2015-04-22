import java.util.*;
import java.util.concurrent.Callable;

public class Optimizer implements Callable<String> {

    private static class SubSet {

        Selectivities selectivities; // Container for the selectivities of this subset
        int k;                       // Number of terms corresponding to each subset.
        double p;                    // Product of the values of all terms in the subset.
        boolean b;                   // Whether the no-branch optimization was used to get the best cost.
        double c;                    // Current best cost for the subset.
        SubSet L;                    // Left child of the sub plan.
        SubSet R;                    // Right child of the sub plan.

        public static class CostModel {
            double r; // the cost of accessing an array element rj[i] in order to perform operations on it
            double t; // the cost of performing an if test
            double l; // the cost of performing a logical “and”
            double m; // the cost of a branch misprediction
            double a; // the cost of writing an answer to the answer array and incrementing the answer array counter
            double f; // the cost of applying function f to its argument

            public CostModel(double a, double f, double l, double m, double r, double t) {
                this.a = a;
                this.f = f;
                this.l = l;
                this.m = m;
                this.r = r;
                this.t = t;
            }
        }

        public static CostModel costModel;

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
            return one.fixedCost() + costModel.m * q + one.p * two.c;
        }

        public SubSet leftMost() {
            SubSet q = this;
            while (q.L != null) {
                q = q.L;
            }
            return q;
        }

        public boolean lemma48(SubSet two) {
            double p1 = two.p;
            double p2 = this.leftMost().p;
            return p2 <= p1 && ((p2 - 1) / this.leftMost().fixedCost()) < ((p1 - 1) / two.fixedCost());
        }

        public boolean lemma49(SubSet two) {
            double p1 = two.p;
            double p2 = this.leftMost().p;
            return p2 <= p1 && this.leftMost().fixedCost() < two.fixedCost();
        }
    }

    private final List<Double> selectivities;
    private List<SubSet> subSets;

    public Optimizer(List<Double> selectivities, Properties props) {
        this.selectivities = selectivities;
        this.subSets = generateSubSets(selectivities);
        SubSet.costModel = parseCostModel(props);
        initializeCosts(subSets);
    }

    private SubSet.CostModel parseCostModel(Properties props) {
        double r = Double.valueOf(props.getProperty("r"));
        double t = Double.valueOf(props.getProperty("t"));
        double l = Double.valueOf(props.getProperty("l"));
        double m = Double.valueOf(props.getProperty("m"));
        double a = Double.valueOf(props.getProperty("a"));
        double f = Double.valueOf(props.getProperty("f"));
        return new SubSet.CostModel(a, f, l, m, r, t);
    }

    /**
     * Implements algorithm 4.11.
     * @return C code that computes the optimal cost of a query given a selectivity.
     * @throws Exception if things things go bananas.
     */
    @Override
    public String call() throws Exception {
        for (SubSet s : subSets) {
            for (SubSet sPrime : subSets) {
                if (!bitIntersect(s.selectivities.bitSet, sPrime.selectivities.bitSet)) {
                    if (!(s.lemma48(sPrime) || (sPrime.p <= 0.5 && s.lemma49(sPrime)))) {
                        double c = SubSet.combinedCost(sPrime, s);
                        List<Boolean> union = bitUnion(sPrime.selectivities.bitSet, s.selectivities.bitSet);
                        SubSet subset = findSubSetByBitSet(subSets, union);
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

    /**
     * Iterates over the global list of subsets and finds the one that matches the given bit set.
     *
     * @param allSubSets The global list of subsets.
     * @param bitSet The bit set to look for.
     * @return A subset that contains the specified bit set.
     */
    private static SubSet findSubSetByBitSet(List<SubSet> allSubSets, List<Boolean> bitSet) {
        return allSubSets
                .stream()
                .filter(ss -> ss.selectivities.bitSet.equals(bitSet))
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

    private static List<SubSet> generateSubSets(List<Double> selectivities) {
        int subSetSize = (int) (Math.pow(2.0, selectivities.size()) - 1);

        // gets a list initialized to (0, 1, 2... selectivities.size - 1)

        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < selectivities.size(); i++) {
             numbers.add(i);
        }
        // generates the powerset for the list
        List<List<Integer>> positions = orderedPowerSet(numbers);
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

            subSets.add(subSet);
        }
        return subSets;
    }

    /**
     * Constructs a bit set with on bits set at the specified positions.
     *
     * @param positions The list of bits to initialize.
     * @param length The total size of the bit set.
     * @return A bit set with bits in 'positions' turned on.
     */
    private static List<Boolean> bitSetFromPositions(List<Integer> positions, int length) {
        List<Boolean> bitSet = new ArrayList<>(Collections.nCopies(length, false));
        positions.stream().forEach(i -> bitSet.set(i, true));
        return bitSet;
    }

    /**
     * Initializes the logical and no-branching cost for each SubSet.
     *
     * @param subSets The SubSets to initialize;
     */
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

    /**
     * Creates a standard powerset from a given set.
     *
     * @param originalSet The starting set.
     * @param <T> Type of the set.
     * @return The powerset of the original set.
     */
    public static <T> List<List<T>> powerSet(List<T> originalSet) {
        if (originalSet.isEmpty()) {
            return Collections.singletonList(Collections.emptyList());
        }
        List<List<T>> result = new ArrayList<>();
        List<T> list = new ArrayList<>(originalSet);
        T head = list.get(0);
        List<T> rest = new ArrayList<>(list.subList(1, list.size()));
        for (List<T> set : powerSet(rest)) {
            List<T> newSet = new ArrayList<>();
            newSet.add(head);
            newSet.addAll(set);
            result.add(newSet);
            result.add(set);
        }
        return result;
    }

    public static <T> List<List<T>> orderedPowerSet(List<T> originalSet) {
        return order(powerSet(originalSet));
    }

    public static class Selectivities {
        List<Boolean> bitSet;     // The bit set that represents selectivities
        List<Double> values;      // The actual value of the selectivities, mapped by the bit set

        public Selectivities(List<Boolean> bitSet, List<Double> values) {
            this.bitSet = bitSet;
            this.values = values;
        }

        /**
         * Gets the set of selectivities for the current bit set.
         *
         * @return A set of selectivities that match the corresponding bit set.
         */
        public List<Double> getSelectivitiesFromBits() {
            List<Double> rs = new ArrayList<>(Collections.nCopies(values.size(), 0.0));
            for (int i = 0; i < bitSet.size(); i++) {
                if (bitSet.get(i)) {
                    rs.set(i, values.get(i));
                }
            }
            return rs;
        }

        /**
         * Calculates the p value for this bit set.
         *
         * @return The product of selectivities in this bit set.
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

    /**
     * The union of two bit sets.
     *
     * @param first A bit set.
     * @param second Another bit set.
     * @return A new bit set that has joined bitSet turned on.
     */
    public static List<Boolean> bitUnion(List<Boolean> first, List<Boolean> second) {
        List<Boolean> union = new ArrayList<>(first.size());
        for (int i = 0; i < first.size(); i++) {
            union.add(first.get(i) || second.get(i));
        }
        return union;
    }

    /**
     * The intersection of two bit sets.
     *
     * @param first A bit set.
     * @param second Another bit set.
     * @return A new bit set that has intersected bitSet turned on.
     */
    public static List<Boolean> bitIntersection(List<Boolean> first, List<Boolean> second) {
        List<Boolean> intersection = new ArrayList<>(first.size());
        for (int i = 0; i < first.size(); i++) {
            intersection.add(first.get(i) && second.get(i));
        }
        return intersection;
    }

    /**
     * Tests whether two bit sets intersect.
     *
     * @param first A bit set.
     * @param second Another bit set.
     * @return true if there is an intersection between two bit sets, false otherwise.
     */
    public static boolean bitIntersect(List<Boolean> first, List<Boolean> second) {
        return bitIntersection(first, second).contains(true);
    }
}
