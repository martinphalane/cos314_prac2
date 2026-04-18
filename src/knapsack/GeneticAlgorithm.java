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
 *                   mutationRate (bit-flip mutation).
 *   4. REPAIR     – infeasible chromosomes are repaired greedily: items
 *                   are removed in ascending value/weight ratio order
 *                   until the weight constraint is satisfied.
 *   5. ELITISM    – the best individual from the current generation is
 *                   always carried over to the next unchanged.
 *
 * CONFIGURATION (scales with instance size n):
 *   Population size   : max(100, 10*n)  – larger populations for larger instances
 *   Max generations   : max(500, 50*n)  – more generations for larger instances
 *   Crossover rate    : 0.85
 *   Mutation rate     : 1.0/n  (one expected flip per chromosome)
 *   Selection         : binary tournament (k = 2)
 *   Elitism           : top-1 individual preserved
 * =============================================================================
 */
public class GeneticAlgorithm {

    /* ------------------------------------------------------------------ */
    /*  Configuration constants                                             */
    /* ------------------------------------------------------------------ */

    public static final double CROSSOVER_RATE   = 0.85;
    public static final int    TOURNAMENT_SIZE  = 2;

    /* ------------------------------------------------------------------ */
    /*  Fields                                                              */
    /* ------------------------------------------------------------------ */

    private final KnapsackInstance instance;
    private final Random            rng;

    /* Adaptive parameters set in solve() based on n */
    private int    populationSize;
    private int    maxGenerations;
    private double mutationRate;

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
     * Parameters are scaled adaptively based on instance size.
     */
    public int solve() {
        int n = instance.getNumItems();

        /* Adaptive parameter scaling */
        populationSize = Math.min(Math.max(100, 5 * n), 200);
        maxGenerations = Math.min(Math.max(500, 20 * n), 2000);
        mutationRate   = 1.0 / n;   // expected ~1 flip per chromosome

        /* ---- Initialise population with mixed greedy+random ---- */
        boolean[][] population = initialisePopulation(n);
        repairPopulation(population);

        bestChromosome = new boolean[n];
        bestFitness    = Integer.MIN_VALUE;

        /* ---- Generational loop ---- */
        for (int gen = 0; gen < maxGenerations; gen++) {

            /* Evaluate fitnesses */
            int[] fitnesses = evaluatePopulation(population);

            /* Track best */
            for (int i = 0; i < populationSize; i++) {
                if (fitnesses[i] > bestFitness) {
                    bestFitness    = fitnesses[i];
                    bestChromosome = population[i].clone();
                }
            }

            /* Build next generation */
            boolean[][] nextGen  = new boolean[populationSize][n];
            int         eliteIdx = argMax(fitnesses);
            nextGen[0]           = population[eliteIdx].clone(); // elitism

            for (int i = 1; i < populationSize; i += 2) {

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
                if (i + 1 < populationSize) {
                    nextGen[i + 1] = child2;
                }
            }

            /* Repair infeasible solutions */
            repairPopulation(nextGen);

            population = nextGen;
        }

        /* Final evaluation pass */
        int[] finalFitnesses = evaluatePopulation(population);
        for (int i = 0; i < populationSize; i++) {
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
    /*  Initialisation – mixed greedy + random seeding                     */
    /* ------------------------------------------------------------------ */

    /**
     * Creates a population with 20% greedy-seeded individuals and 80% random.
     * Seeding the population with high-quality greedy solutions accelerates
     * convergence, while keeping 80% random preserves diversity.
     */
    private boolean[][] initialisePopulation(int n) {
        boolean[][] pop = new boolean[populationSize][n];

        /* Greedy seed: top 20% of population */
        int greedyCount = Math.max(1, populationSize / 5);
        boolean[] greedySeed = greedyInitial(n);
        for (int i = 0; i < greedyCount; i++) {
            pop[i] = perturbGreedy(greedySeed, n, 0.1);
        }

        /* Random individuals: remaining 80% */
        for (int i = greedyCount; i < populationSize; i++) {
            for (int j = 0; j < n; j++) {
                pop[i][j] = rng.nextBoolean();
            }
        }
        return pop;
    }

    /** Greedy solution sorted by descending value/weight ratio. */
    private boolean[] greedyInitial(int n) {
        boolean[] sol = new boolean[n];
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(
            (double) instance.getValue(b) / instance.getWeight(b),
            (double) instance.getValue(a) / instance.getWeight(a)));
        int remaining = instance.getCapacity();
        for (int i : idx) {
            if (instance.getWeight(i) <= remaining) {
                sol[i] = true;
                remaining -= instance.getWeight(i);
            }
        }
        return sol;
    }

    /** Randomly flips each bit in the greedy seed with given probability. */
    private boolean[] perturbGreedy(boolean[] seed, int n, double prob) {
        boolean[] sol = seed.clone();
        for (int i = 0; i < n; i++) {
            if (rng.nextDouble() < prob) sol[i] = !sol[i];
        }
        return sol;
    }

    /* ------------------------------------------------------------------ */
    /*  Evaluation                                                          */
    /* ------------------------------------------------------------------ */

    private int[] evaluatePopulation(boolean[][] population) {
        int[] fitnesses = new int[populationSize];
        for (int i = 0; i < populationSize; i++) {
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
        int best = rng.nextInt(populationSize);
        for (int k = 1; k < TOURNAMENT_SIZE; k++) {
            int challenger = rng.nextInt(populationSize);
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
     * A random cut-point is chosen in [1, n-1].
     */
    private boolean[][] singlePointCrossover(boolean[] p1, boolean[] p2) {
        int n        = p1.length;
        int cutPoint = 1 + rng.nextInt(n - 1);

        boolean[] c1 = new boolean[n];
        boolean[] c2 = new boolean[n];

        for (int i = 0; i < n; i++) {
            if (i < cutPoint) { c1[i] = p1[i]; c2[i] = p2[i]; }
            else               { c1[i] = p2[i]; c2[i] = p1[i]; }
        }
        return new boolean[][] {c1, c2};
    }

    /* ------------------------------------------------------------------ */
    /*  Mutation – Bit-Flip                                                 */
    /* ------------------------------------------------------------------ */

    /**
     * Flips each bit independently with probability mutationRate (= 1/n).
     * This gives on average exactly one mutation per chromosome, which is
     * the standard recommendation for binary-encoded GAs.
     */
    private void mutate(boolean[] chromosome) {
        for (int i = 0; i < chromosome.length; i++) {
            if (rng.nextDouble() < mutationRate) {
                chromosome[i] = !chromosome[i];
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Repair Operator                                                     */
    /* ------------------------------------------------------------------ */

    private void repairPopulation(boolean[][] population) {
        for (boolean[] chrom : population) repair(chrom);
    }

    /**
     * Repairs a single chromosome by removing items in ascending
     * value/weight ratio order until the weight constraint is satisfied.
     */
    private void repair(boolean[] chromosome) {
        int n = instance.getNumItems();
        while (instance.totalWeight(chromosome) > instance.getCapacity()) {
            int    worstIdx   = -1;
            double worstRatio = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (chromosome[i]) {
                    double ratio = (double) instance.getValue(i) / instance.getWeight(i);
                    if (ratio < worstRatio) {
                        worstRatio = ratio;
                        worstIdx   = i;
                    }
                }
            }
            if (worstIdx == -1) break;
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
