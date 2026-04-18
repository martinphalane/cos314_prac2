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
 *   s0  <- GenerateInitialSolution()     // greedy construction
 *   s*  <- LocalSearch(s0)               // find initial local optimum
 *   REPEAT:
 *     s'      <- Perturb(s*)             // random k-bit flip
 *     s'      <- Repair(s')             // restore feasibility
 *     s_local <- LocalSearch(s')         // descend to local optimum
 *     s*      <- Accept(s*, s_local)     // strict improvement
 *   UNTIL stopping criterion met
 *
 * LOCAL SEARCH – Best-Improvement 1-Bit Flip:
 *   Exhaustively evaluates all n neighbours obtained by flipping one bit.
 *   Moves to the best improving neighbour; repeats until no improvement.
 *
 * PERTURBATION – Random k-Bit Flip:
 *   Randomly flips perturbationStrength bits (= max(4, ceil(0.05 * n))).
 *
 * ACCEPTANCE – Strict Improvement:
 *   The new local optimum replaces the incumbent only if strictly better.
 *
 * CONFIGURATION (scales with instance size n):
 *   Max iterations          : max(1000, 100*n)
 *   Perturbation strength   : max(4, ceil(0.05 * n))  bits flipped
 * =============================================================================
 */
public class IteratedLocalSearch {

    /* ------------------------------------------------------------------ */
    /*  Fields                                                              */
    /* ------------------------------------------------------------------ */

    private final KnapsackInstance instance;
    private final Random            rng;

    /* Adaptive parameters set in solve() */
    private int maxIterations;
    private int perturbationStrength;

    private boolean[] bestSolution;
    private int        bestFitness;

    /* ------------------------------------------------------------------ */
    /*  Constructor                                                         */
    /* ------------------------------------------------------------------ */

    public IteratedLocalSearch(KnapsackInstance instance, long seed) {
        this.instance = instance;
        this.rng      = new Random(seed);
    }

    /* ------------------------------------------------------------------ */
    /*  Public API                                                          */
    /* ------------------------------------------------------------------ */

    public int solve() {
        int n = instance.getNumItems();

        /* Adaptive parameters */
        maxIterations        = Math.min(Math.max(1000, 20 * n), 3000);
        perturbationStrength = Math.max(4, (int) Math.ceil(0.05 * n));

        /* ---- Step 1: Greedy initial solution ---- */
        boolean[] current = generateInitialSolution(n);
        repair(current);

        /* ---- Step 2: Descend to initial local optimum ---- */
        current      = localSearch(current);
        bestFitness  = instance.evaluate(current);
        bestSolution = current.clone();

        /* ---- Step 3: ILS main loop ---- */
        for (int iter = 0; iter < maxIterations; iter++) {

            boolean[] perturbed = perturb(bestSolution);
            repair(perturbed);

            boolean[] localOpt   = localSearch(perturbed);
            int        localValue = instance.evaluate(localOpt);

            if (localValue > bestFitness) {
                bestFitness  = localValue;
                bestSolution = localOpt.clone();
            }
        }

        return bestFitness;
    }

    public boolean[] getBestSolution() { return bestSolution.clone(); }
    public int       getBestFitness()  { return bestFitness; }

    /* ------------------------------------------------------------------ */
    /*  Initial Solution – Greedy Construction                             */
    /* ------------------------------------------------------------------ */

    /**
     * Constructs an initial solution by sorting items by descending
     * value/weight ratio and adding greedily while feasible.
     */
    private boolean[] generateInitialSolution(int n) {
        boolean[] sol = new boolean[n];
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Double.compare(
            (double) instance.getValue(b) / instance.getWeight(b),
            (double) instance.getValue(a) / instance.getWeight(a)));

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
     * Best-improvement hill climber. Evaluates all n 1-bit-flip neighbours
     * and moves to the best improving one; repeats until no improvement.
     */
    private boolean[] localSearch(boolean[] start) {
        int n         = instance.getNumItems();
        boolean[] cur = start.clone();
        int curFit    = instance.evaluate(cur);

        boolean improved = true;
        while (improved) {
            improved = false;
            int bestNeighbourFit = curFit;
            int bestFlipIdx      = -1;

            for (int i = 0; i < n; i++) {
                cur[i] = !cur[i];
                int newFit;

                if (instance.totalWeight(cur) <= instance.getCapacity()) {
                    newFit = instance.evaluate(cur);
                } else {
                    boolean[] temp = cur.clone();
                    repair(temp);
                    newFit = instance.evaluate(temp);
                }

                if (newFit > bestNeighbourFit) {
                    bestNeighbourFit = newFit;
                    bestFlipIdx      = i;
                }
                cur[i] = !cur[i];   // undo flip
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
     * Randomly flips perturbationStrength (= max(4, ceil(0.05 * n))) bits.
     * Flipping ~5% of genes creates jumps large enough to escape local
     * optima while retaining useful solution structure.
     */
    private boolean[] perturb(boolean[] solution) {
        int       n         = solution.length;
        boolean[] perturbed = solution.clone();
        for (int k = 0; k < perturbationStrength; k++) {
            perturbed[rng.nextInt(n)] ^= true;
        }
        return perturbed;
    }

    /* ------------------------------------------------------------------ */
    /*  Repair Operator                                                     */
    /* ------------------------------------------------------------------ */

    private void repair(boolean[] solution) {
        int n = instance.getNumItems();
        while (instance.totalWeight(solution) > instance.getCapacity()) {
            int    worstIdx   = -1;
            double worstRatio = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (solution[i]) {
                    double ratio = (double) instance.getValue(i) / instance.getWeight(i);
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
