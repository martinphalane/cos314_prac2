package knapsack;

import java.util.Arrays;
import java.util.Random;

/**
 * =============================================================================
 *  Iterated Local Search (ILS) for the 0/1 Knapsack Problem
 * =============================================================================
 *
 * ALGORITHM OVERVIEW (Russell & Norvig, Chapter 4 – Local Search):
 * ----------------------------------------------------------------
 * Iterated Local Search is a trajectory-based meta-heuristic that
 * extends simple hill-climbing by escaping local optima through a
 * PERTURBATION step followed by a fresh LOCAL SEARCH phase.
 *
 * The algorithm operates as follows:
 *
 *   s0 ← GenerateInitialSolution()      // greedy construction
 *   s* ← LocalSearch(s0)                // find initial local optimum
 *   REPEAT:
 *     s' ← Perturb(s*, history)         // random double-bridge / k-bit flip
 *     s' ← Repair(s')                   // restore feasibility
 *     s_local ← LocalSearch(s')         // descend to local optimum
 *     s* ← AcceptanceCriterion(s*, s_local)  // accept if improvement
 *   UNTIL stopping criterion met
 *
 * LOCAL SEARCH – Best-Improvement 1-bit Flip:
 *   Exhaustively evaluates all n neighbours obtained by flipping one bit.
 *   Moves to the best improving neighbour; repeats until no improvement.
 *
 * PERTURBATION – Random k-Bit Flip:
 *   Randomly flips PERTURBATION_STRENGTH bits (default 4).  This creates
 *   a "jump" in the search space large enough to escape the current local
 *   optimum basin of attraction while remaining close enough to retain
 *   useful structure.
 *
 * ACCEPTANCE – Strict Improvement:
 *   The new local optimum replaces the incumbent only if it is strictly
 *   better (greedy acceptance).
 *
 * REPAIR:
 *   Identical to the GA repair – remove items in ascending value/weight
 *   ratio order until the weight constraint is satisfied.
 *
 * CONFIGURATION (tuned empirically):
 *   Max iterations          : 1000
 *   Perturbation strength   : 4  (number of bits flipped)
 *   Local search restarts   : best-improvement exhaustive flip
 * =============================================================================
 */
public class IteratedLocalSearch {

    /* ------------------------------------------------------------------ */
    /*  Configuration constants                                             */
    /* ------------------------------------------------------------------ */

    public static final int MAX_ITERATIONS         = 1000;
    public static final int PERTURBATION_STRENGTH  = 4;

    /* ------------------------------------------------------------------ */
    /*  Fields                                                              */
    /* ------------------------------------------------------------------ */

    private final KnapsackInstance instance;
    private final Random            rng;

    private boolean[] bestSolution;
    private int        bestFitness;

    /* ------------------------------------------------------------------ */
    /*  Constructor                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * @param instance the problem instance to solve
     * @param seed     random seed for reproducibility
     */
    public IteratedLocalSearch(KnapsackInstance instance, long seed) {
        this.instance = instance;
        this.rng      = new Random(seed);
    }

    /* ------------------------------------------------------------------ */
    /*  Public API                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Runs the ILS and returns the best fitness value found.
     */
    public int solve() {
        int n = instance.getNumItems();

        /* ---- Step 1: Generate and repair initial solution ---- */
        boolean[] current = generateInitialSolution(n);
        repair(current);

        /* ---- Step 2: Descend to initial local optimum ---- */
        current     = localSearch(current);
        bestFitness = instance.evaluate(current);
        bestSolution = current.clone();

        /* ---- Step 3: ILS main loop ---- */
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {

            /* Perturbation: random k-bit flip on the best solution */
            boolean[] perturbed = perturb(bestSolution);
            repair(perturbed);

            /* Local search from perturbed solution */
            boolean[] localOpt    = localSearch(perturbed);
            int        localValue  = instance.evaluate(localOpt);

            /* Acceptance: strict improvement only */
            if (localValue > bestFitness) {
                bestFitness  = localValue;
                bestSolution = localOpt.clone();
            }
        }

        return bestFitness;
    }

    /** Returns the best solution found after {@link #solve()} is called. */
    public boolean[] getBestSolution() { return bestSolution.clone(); }

    /** Returns the best fitness found after {@link #solve()} is called. */
    public int getBestFitness() { return bestFitness; }

    /* ------------------------------------------------------------------ */
    /*  Initial Solution – Greedy Construction                             */
    /* ------------------------------------------------------------------ */

    /**
     * Constructs an initial solution using a greedy heuristic:
     * items are sorted by descending value/weight ratio and added
     * greedily as long as they fit in the knapsack.
     *
     * A greedy start gives ILS a better initial local optimum than a
     * purely random solution would, accelerating convergence.
     */
    private boolean[] generateInitialSolution(int n) {
        boolean[] sol = new boolean[n];

        /* Create index array sorted by value/weight ratio descending */
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> {
            double ra = (double) instance.getValue(a) / instance.getWeight(a);
            double rb = (double) instance.getValue(b) / instance.getWeight(b);
            return Double.compare(rb, ra);  // descending
        });

        int remaining = instance.getCapacity();
        for (int idx : indices) {
            if (instance.getWeight(idx) <= remaining) {
                sol[idx]   = true;
                remaining -= instance.getWeight(idx);
            }
        }
        return sol;
    }

    /* ------------------------------------------------------------------ */
    /*  Local Search – Best-Improvement 1-Bit Flip                         */
    /* ------------------------------------------------------------------ */

    /**
     * Best-improvement hill climber.
     * At each step all n 1-bit-flip neighbours are evaluated; the best
     * improving neighbour is accepted.  Repeats until no single flip
     * improves the objective.
     *
     * Best-improvement is preferred here over first-improvement because
     * the knapsack neighbourhood is small (size n) and evaluating all
     * neighbours deterministically avoids missing the steepest ascent.
     */
    private boolean[] localSearch(boolean[] start) {
        int n         = instance.getNumItems();
        boolean[] cur = start.clone();
        int curFit    = instance.evaluate(cur);

        boolean improved = true;
        while (improved) {
            improved = false;
            int    bestNeighbourFit = curFit;
            int    bestFlipIdx      = -1;

            for (int i = 0; i < n; i++) {
                /* Flip bit i */
                cur[i] = !cur[i];
                int newFit = 0;

                /* Only evaluate feasible flips (or repair implicitly) */
                if (instance.totalWeight(cur) <= instance.getCapacity()) {
                    newFit = instance.evaluate(cur);
                } else {
                    /* Temporarily repair to check value potential */
                    boolean[] temp = cur.clone();
                    repair(temp);
                    newFit = instance.evaluate(temp);
                }

                if (newFit > bestNeighbourFit) {
                    bestNeighbourFit = newFit;
                    bestFlipIdx      = i;
                }

                /* Undo flip */
                cur[i] = !cur[i];
            }

            if (bestFlipIdx != -1) {
                cur[bestFlipIdx] = !cur[bestFlipIdx];
                repair(cur);
                curFit   = instance.evaluate(cur);
                improved = true;
            }
        }
        return cur;
    }

    /* ------------------------------------------------------------------ */
    /*  Perturbation – Random k-Bit Flip                                   */
    /* ------------------------------------------------------------------ */

    /**
     * Perturbs the current best solution by randomly flipping
     * PERTURBATION_STRENGTH bits.  This "double-bridge"-equivalent move
     * changes multiple bits simultaneously, creating jumps that single
     * 1-bit local search steps cannot undo easily – preventing ILS from
     * immediately returning to the same local optimum.
     */
    private boolean[] perturb(boolean[] solution) {
        int       n         = solution.length;
        boolean[] perturbed = solution.clone();

        for (int k = 0; k < PERTURBATION_STRENGTH; k++) {
            int flipIdx           = rng.nextInt(n);
            perturbed[flipIdx]    = !perturbed[flipIdx];
        }
        return perturbed;
    }

    /* ------------------------------------------------------------------ */
    /*  Repair Operator (same strategy as GA)                               */
    /* ------------------------------------------------------------------ */

    /**
     * Repairs an infeasible solution by removing items with the
     * lowest value/weight ratio until the weight constraint is met.
     */
    private void repair(boolean[] solution) {
        int n = instance.getNumItems();
        while (instance.totalWeight(solution) > instance.getCapacity()) {
            int    worstIdx   = -1;
            double worstRatio = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (solution[i]) {
                    double ratio = (double) instance.getValue(i)
                            / instance.getWeight(i);
                    if (ratio < worstRatio) {
                        worstRatio = ratio;
                        worstIdx   = i;
                    }
                }
            }
            if (worstIdx == -1) break;
            solution[worstIdx] = false;
        }
    }
}