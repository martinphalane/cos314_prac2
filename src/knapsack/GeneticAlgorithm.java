package knapsack;

import java.util.Arrays;
import java.util.Random;

/**
 * =============================================================================
 *  Genetic Algorithm (GA) for the 0/1 Knapsack Problem
 * =============================================================================
 *
 * ALGORITHM OVERVIEW (Russell & Norvig, Chapter 4 – Local Search):
 * ----------------------------------------------------------------
 * A Genetic Algorithm is a population-based meta-heuristic that mimics
 * biological evolution.  Each candidate solution is encoded as a
 * chromosome – here a binary array of length n where bit i = 1 means
 * item i is included in the knapsack.
 *
 * The algorithm iterates through a fixed number of generations:
 *   1. SELECTION  – fitter individuals are more likely to be chosen as
 *                   parents (binary tournament selection).
 *   2. CROSSOVER  – two parents exchange genetic material at a random
 *                   cut-point (single-point crossover), producing two
 *                   offspring.
 *   3. MUTATION   – each gene is flipped independently with probability
 *                   MUTATION_RATE (bit-flip mutation).
 *   4. REPAIR     – infeasible chromosomes are repaired greedily: items
 *                   are removed in ascending value/weight ratio order
 *                   until the weight constraint is satisfied.
 *   5. ELITISM    – the best individual from the current generation is
 *                   always carried over to the next unchanged.
 *
 * CONFIGURATION (tuned empirically):
 *   Population size   : 100
 *   Max generations   : 500
 *   Crossover rate    : 0.85
 *   Mutation rate     : 0.02  (per gene)
 *   Selection         : binary tournament (k = 2)
 *   Elitism           : top-1 individual preserved
 * =============================================================================
 */
public class GeneticAlgorithm {

    /* ------------------------------------------------------------------ */
    /*  Configuration constants                                             */
    /* ------------------------------------------------------------------ */

    public static final int    POPULATION_SIZE  = 100;
    public static final int    MAX_GENERATIONS  = 500;
    public static final double CROSSOVER_RATE   = 0.85;
    public static final double MUTATION_RATE    = 0.02;
    public static final int    TOURNAMENT_SIZE  = 2;

    /* ------------------------------------------------------------------ */
    /*  Fields                                                              */
    /* ------------------------------------------------------------------ */

    private final KnapsackInstance instance;
    private final Random            rng;

    /** Best solution found. */
    private boolean[] bestChromosome;
    private int        bestFitness;

    /* ------------------------------------------------------------------ */
    /*  Constructor                                                         */
    /* ------------------------------------------------------------------ */

    /**
     * @param instance the problem instance to solve
     * @param seed     random seed for reproducibility
     */
    public GeneticAlgorithm(KnapsackInstance instance, long seed) {
        this.instance = instance;
        this.rng      = new Random(seed);
    }

    /* ------------------------------------------------------------------ */
    /*  Public API                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Runs the GA and returns the best fitness value found.
     */
    public int solve() {
        int n = instance.getNumItems();

        /* ---- Initialise population ---- */
        boolean[][] population = initialisePopulation(n);
        repairPopulation(population);

        bestChromosome = new boolean[n];
        bestFitness    = Integer.MIN_VALUE;

        /* ---- Generational loop ---- */
        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {

            /* Evaluate fitnesses */
            int[] fitnesses = evaluatePopulation(population);

            /* Track best */
            for (int i = 0; i < POPULATION_SIZE; i++) {
                if (fitnesses[i] > bestFitness) {
                    bestFitness    = fitnesses[i];
                    bestChromosome = population[i].clone();
                }
            }

            /* Build next generation */
            boolean[][] nextGen    = new boolean[POPULATION_SIZE][n];
            int         eliteIdx   = argMax(fitnesses);
            nextGen[0]             = population[eliteIdx].clone(); // elitism

            for (int i = 1; i < POPULATION_SIZE; i += 2) {

                /* Selection */
                boolean[] parent1 = tournamentSelect(population, fitnesses);
                boolean[] parent2 = tournamentSelect(population, fitnesses);

                boolean[] child1, child2;

                /* Crossover */
                if (rng.nextDouble() < CROSSOVER_RATE) {
                    boolean[][] offspring = singlePointCrossover(parent1, parent2);
                    child1 = offspring[0];
                    child2 = offspring[1];
                } else {
                    child1 = parent1.clone();
                    child2 = parent2.clone();
                }

                /* Mutation */
                mutate(child1);
                mutate(child2);

                nextGen[i] = child1;
                if (i + 1 < POPULATION_SIZE) {
                    nextGen[i + 1] = child2;
                }
            }

            /* Repair infeasible solutions */
            repairPopulation(nextGen);

            population = nextGen;
        }

        /* Final evaluation pass */
        int[] finalFitnesses = evaluatePopulation(population);
        for (int i = 0; i < POPULATION_SIZE; i++) {
            if (finalFitnesses[i] > bestFitness) {
                bestFitness    = finalFitnesses[i];
                bestChromosome = population[i].clone();
            }
        }

        return bestFitness;
    }

    /** Returns the best chromosome found after {@link #solve()} is called. */
    public boolean[] getBestChromosome() { return bestChromosome.clone(); }

    /** Returns the best fitness value found after {@link #solve()} is called. */
    public int getBestFitness() { return bestFitness; }

    /* ------------------------------------------------------------------ */
    /*  Initialisation                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Creates a random population.  Each gene is set to true with
     * probability 0.5 (uniform random binary encoding).
     */
    private boolean[][] initialisePopulation(int n) {
        boolean[][] pop = new boolean[POPULATION_SIZE][n];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            for (int j = 0; j < n; j++) {
                pop[i][j] = rng.nextBoolean();
            }
        }
        return pop;
    }

    /* ------------------------------------------------------------------ */
    /*  Evaluation                                                          */
    /* ------------------------------------------------------------------ */

    private int[] evaluatePopulation(boolean[][] population) {
        int[] fitnesses = new int[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            fitnesses[i] = instance.evaluate(population[i]);
        }
        return fitnesses;
    }

    /* ------------------------------------------------------------------ */
    /*  Selection – Binary Tournament                                       */
    /* ------------------------------------------------------------------ */

    /**
     * Selects one parent via binary tournament selection:
     * randomly pick TOURNAMENT_SIZE individuals and return the fittest.
     */
    private boolean[] tournamentSelect(boolean[][] population, int[] fitnesses) {
        int best = rng.nextInt(POPULATION_SIZE);
        for (int k = 1; k < TOURNAMENT_SIZE; k++) {
            int challenger = rng.nextInt(POPULATION_SIZE);
            if (fitnesses[challenger] > fitnesses[best]) {
                best = challenger;
            }
        }
        return population[best].clone();
    }

    /* ------------------------------------------------------------------ */
    /*  Crossover – Single-Point                                            */
    /* ------------------------------------------------------------------ */

    /**
     * Performs single-point crossover between two parents.
     * A random cut-point is chosen in [1, n-1].  Child1 takes genes
     * [0..cut-1] from parent1 and [cut..n-1] from parent2; child2
     * takes the complementary segments.
     */
    private boolean[][] singlePointCrossover(boolean[] p1, boolean[] p2) {
        int n        = p1.length;
        int cutPoint = 1 + rng.nextInt(n - 1);   // cut in [1, n-1]

        boolean[] c1 = new boolean[n];
        boolean[] c2 = new boolean[n];

        for (int i = 0; i < n; i++) {
            if (i < cutPoint) {
                c1[i] = p1[i];
                c2[i] = p2[i];
            } else {
                c1[i] = p2[i];
                c2[i] = p1[i];
            }
        }
        return new boolean[][] {c1, c2};
    }

    /* ------------------------------------------------------------------ */
    /*  Mutation – Bit-Flip                                                 */
    /* ------------------------------------------------------------------ */

    /**
     * Flips each bit independently with probability MUTATION_RATE.
     */
    private void mutate(boolean[] chromosome) {
        for (int i = 0; i < chromosome.length; i++) {
            if (rng.nextDouble() < MUTATION_RATE) {
                chromosome[i] = !chromosome[i];
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Repair Operator                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * Repairs all infeasible solutions in the population.
     * Infeasible solutions (overweight) are fixed by removing items in
     * ascending value/weight ratio order until the weight constraint
     * is satisfied.  This greedy repair is computationally cheap and
     * preserves as much value as possible.
     */
    private void repairPopulation(boolean[][] population) {
        for (boolean[] chrom : population) {
            repair(chrom);
        }
    }

    /**
     * Repairs a single chromosome.
     *
     * Strategy:
     *   While totalWeight > capacity:
     *     Remove the selected item with the LOWEST value/weight ratio.
     *   (Removes least valuable items first to minimise value loss.)
     */
    private void repair(boolean[] chromosome) {
        int n = instance.getNumItems();

        /* Build a list of selected item indices sorted by value/weight asc */
        while (instance.totalWeight(chromosome) > instance.getCapacity()) {
            int    worstIdx   = -1;
            double worstRatio = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (chromosome[i]) {
                    double ratio = (double) instance.getValue(i)
                            / instance.getWeight(i);
                    if (ratio < worstRatio) {
                        worstRatio = ratio;
                        worstIdx   = i;
                    }
                }
            }
            if (worstIdx == -1) break;   // nothing selected
            chromosome[worstIdx] = false;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Utility                                                             */
    /* ------------------------------------------------------------------ */

    private int argMax(int[] arr) {
        int best = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[best]) best = i;
        }
        return best;
    }
}