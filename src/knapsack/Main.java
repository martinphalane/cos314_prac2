package knapsack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {

    private static final String DATA_DIR = "src/data/Knapsack Instances/";

    private static final Object[][] INSTANCES = {
        { "f1_l-d_kp_10_269",   "f1  l-d_kp_10_269",   0 },
        { "f2_l-d_kp_20_878",   "f2  l-d_kp_20_878",   0 },
        { "f3_l-d_kp_4_20",     "f3  l-d_kp_4_20",     0 },
        { "f4_l-d_kp_4_11",     "f4  l-d_kp_4_11",     0 },
        { "f5_l-d_kp_15_375",   "f5  l-d_kp_15_375",   0 },
        { "f6_l-d_kp_10_60",    "f6  l-d_kp_10_60",    0 },
        { "f7_l-d_kp_7_50",     "f7  l-d_kp_7_50",     0 },
        { "f8_l-d_kp_23_10000", "f8  l-d_kp_23_10000", 0 },
        { "f9_l-d_kp_5_80",     "f9  l-d_kp_5_80",     0 },
        { "f10_l-d_kp_20_879",  "f10 l-d_kp_20_879",   0 },
    };

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        
        // Allow user to choose execution mode
        System.out.println("=== Knapsack Problem Solver ===");
        System.out.println("1. Use single seed (user input)");
        System.out.println("2. Generate random seeds (at least 30 iterations)");
        System.out.print("\nEnter your choice (1 or 2): ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline
        
        // Get optional custom file path
        System.out.print("\nEnter custom file path (press Enter to use default): ");
        String customPath = scanner.nextLine().trim();
        
        if (choice == 1) {
            // Single seed mode
            System.out.print("Enter random seed value: ");
            long seed = scanner.nextLong();
            scanner.close();
            runWithSingleSeed(seed, customPath);
        } else {
            // Multiple seeds mode
            System.out.print("Enter number of iterations (minimum 30): ");
            int iterations = scanner.nextInt();
            iterations = Math.max(iterations, 30);
            scanner.close();
            runWithMultipleSeeds(iterations, customPath);
        }
    }

    private static void runWithSingleSeed(long seed, String customPath) throws Exception {
        System.out.println("\nSeed: " + seed);
        System.out.println("Running algorithms on knapsack instances...\n");

        int[] gaResults  = new int[INSTANCES.length];
        int[] ilsResults = new int[INSTANCES.length];

        printTableHeader();

        for (int inst = 0; inst < INSTANCES.length; inst++) {
            String fileName    = (String)  INSTANCES[inst][0];
            String displayName = (String)  INSTANCES[inst][1];
            int    knownOpt    = (Integer) INSTANCES[inst][2];

            String filePath = customPath.isEmpty() ? DATA_DIR + fileName : customPath;

            KnapsackInstance instance;
            try {
                instance = InstanceLoader.load(filePath, fileName);
            } catch (Exception e) {
                System.err.println("ERROR loading " + filePath + ": " + e.getMessage());
                continue;
            }

            // Run ILS
            long ilsStart = System.nanoTime();
            IteratedLocalSearch ils = new IteratedLocalSearch(instance, seed);
            int  ilsBest  = ils.solve();
            double ilsTime = (System.nanoTime() - ilsStart) / 1_000_000_000.0;

            // Run GA
            long gaStart = System.nanoTime();
            GeneticAlgorithm ga = new GeneticAlgorithm(instance, seed);
            int  gaBest  = ga.solve();
            double gaTime = (System.nanoTime() - gaStart) / 1_000_000_000.0;

            gaResults[inst]  = gaBest;
            ilsResults[inst] = ilsBest;

            // Use best found as known opt if we have none from file
            int displayOpt = (knownOpt > 0) ? knownOpt : Math.max(gaBest, ilsBest);

            printRow(displayName, "ILS", seed, ilsBest, displayOpt, ilsTime);
            printRow("",          "GA",  seed, gaBest,  displayOpt, gaTime);
            printRowSeparator();
        }

        System.out.println();
        printWilcoxonSection(gaResults, ilsResults);
    }

    private static void runWithMultipleSeeds(int iterations, String customPath) throws Exception {
        System.out.println("\nRunning " + iterations + " iterations with random seeds...\n");

        // Load instances first
        KnapsackInstance[] instances = new KnapsackInstance[INSTANCES.length];
        for (int inst = 0; inst < INSTANCES.length; inst++) {
            String fileName = (String) INSTANCES[inst][0];
            String filePath = customPath.isEmpty() ? DATA_DIR + fileName : customPath;
            try {
                instances[inst] = InstanceLoader.load(filePath, fileName);
            } catch (Exception e) {
                System.err.println("ERROR loading " + filePath + ": " + e.getMessage());
            }
        }

        // Track results for each seed
        List<SeedResult> seedResults = new ArrayList<>();
        List<DetailedSeedResult> detailedResults = new ArrayList<>();
        Random random = new Random();

        for (int iter = 0; iter < iterations; iter++) {
            long seed = random.nextLong();
            int[] gaResults = new int[INSTANCES.length];
            int[] ilsResults = new int[INSTANCES.length];
            double[] gaTimes = new double[INSTANCES.length];
            double[] ilsTimes = new double[INSTANCES.length];
            double totalTime = 0;

            for (int inst = 0; inst < INSTANCES.length; inst++) {
                if (instances[inst] == null) continue;

                // Run ILS
                long ilsStart = System.nanoTime();
                IteratedLocalSearch ils = new IteratedLocalSearch(instances[inst], seed);
                int ilsBest = ils.solve();
                ilsTimes[inst] = (System.nanoTime() - ilsStart) / 1_000_000_000.0;
                totalTime += ilsTimes[inst];

                // Run GA
                long gaStart = System.nanoTime();
                GeneticAlgorithm ga = new GeneticAlgorithm(instances[inst], seed);
                int gaBest = ga.solve();
                gaTimes[inst] = (System.nanoTime() - gaStart) / 1_000_000_000.0;
                totalTime += gaTimes[inst];

                gaResults[inst] = gaBest;
                ilsResults[inst] = ilsBest;
            }

            SeedResult seedResult = new SeedResult(seed, gaResults, ilsResults, totalTime);
            seedResults.add(seedResult);
            detailedResults.add(new DetailedSeedResult(seed, gaResults, ilsResults, gaTimes, ilsTimes));
            System.out.printf("Iteration %d/%d completed (Seed: %d)%n", iter + 1, iterations, seed);
        }

        // Display summary of all seeds
        System.out.println("\n=== SEED RESULTS SUMMARY ===\n");
        printSeedResultsHeader();
        
        for (SeedResult result : seedResults) {
            double avgGa = 0, avgIls = 0;
            int countValid = 0;
            for (int i = 0; i < result.gaResults.length; i++) {
                if (result.gaResults[i] > 0) {
                    avgGa += result.gaResults[i];
                    avgIls += result.ilsResults[i];
                    countValid++;
                }
            }
            avgGa = countValid > 0 ? avgGa / countValid : 0;
            avgIls = countValid > 0 ? avgIls / countValid : 0;
            
            printSeedResultRow(result.seed, avgGa, avgIls, result.totalTime);
        }
        printRowSeparator();

        // Find best seed
        SeedResult bestSeed = findBestSeed(seedResults);
        DetailedSeedResult bestDetailed = detailedResults.stream()
                .filter(r -> r.seed == bestSeed.seed)
                .findFirst()
                .orElse(null);

        if (bestSeed != null && bestDetailed != null) {
            // Display best seed results in original table format
            System.out.println("\n=== BEST SEED RESULTS ===");
            System.out.println("Seed: " + bestSeed.seed);
            System.out.println("Running algorithms with best seed...\n");

            printTableHeader();

            for (int inst = 0; inst < INSTANCES.length; inst++) {
                if (bestDetailed.gaResults[inst] <= 0) continue;

                String displayName = (String) INSTANCES[inst][1];
                int knownOpt = (Integer) INSTANCES[inst][2];
                
                int displayOpt = (knownOpt > 0) ? knownOpt : Math.max(bestDetailed.gaResults[inst], bestDetailed.ilsResults[inst]);

                printRow(displayName, "ILS", bestSeed.seed, bestDetailed.ilsResults[inst], displayOpt, bestDetailed.ilsTimes[inst]);
                printRow("", "GA", bestSeed.seed, bestDetailed.gaResults[inst], displayOpt, bestDetailed.gaTimes[inst]);
                printRowSeparator();
            }

            System.out.println();
            printWilcoxonSection(bestDetailed.gaResults, bestDetailed.ilsResults);
        }
    }

    private static SeedResult findBestSeed(List<SeedResult> seedResults) {
        SeedResult best = null;
        double bestAvg = Double.NEGATIVE_INFINITY;
        
        for (SeedResult result : seedResults) {
            double avgGa = calculateAverage(result.gaResults);
            double avgIls = calculateAverage(result.ilsResults);
            double combinedAvg = (avgGa + avgIls) / 2;
            
            if (combinedAvg > bestAvg) {
                bestAvg = combinedAvg;
                best = result;
            }
        }
        return best;
    }

    private static double calculateAverage(int[] arr) {
        double sum = 0;
        int count = 0;
        for (int val : arr) {
            if (val > 0) {
                sum += val;
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }

    // ── Table formatting ──────────────────────────────────────────────────

    private static final int W_INST = 22, W_ALG = 11, W_SEED = 20,
                              W_BEST = 15, W_OPT  = 15, W_TIME = 18;

    private static String hLine() {
        return "+" + rep('-', W_INST+1) + "+" + rep('-', W_ALG+1)  +
               "+" + rep('-', W_SEED+1) + "+" + rep('-', W_BEST+1) +
               "+" + rep('-', W_OPT+1)  + "+" + rep('-', W_TIME+1) + "+";
    }

    private static void printTableHeader() {
        System.out.println(hLine());
        System.out.printf("| %-"+W_INST+"s| %-"+W_ALG+"s| %-"+W_SEED+
                          "s| %-"+W_BEST+"s| %-"+W_OPT+"s| %-"+W_TIME+"s|%n",
                "Problem Instance","Algorithm","Seed Value",
                "Best Solution","Known Optimum","Runtime (seconds)");
        System.out.println(hLine());
    }

    private static void printSeedResultsHeader() {
        System.out.println("+---------------------+---------------------+---------------------+------------------+");
        System.out.printf("| %-19s | %-19s | %-19s | %-16s |%n",
                "Seed Value", "Avg GA Result", "Avg ILS Result", "Time (seconds)");
        System.out.println("+---------------------+---------------------+---------------------+------------------+");
    }

    private static void printSeedResultRow(long seed, double avgGa, double avgIls, double time) {
        System.out.printf("| %-19d | %-19.2f | %-19.2f | %-16.4f |%n", 
                seed, avgGa, avgIls, time);
    }

    private static void printRow(String inst, String alg, long seed,
                                  int best, int opt, double t) {
        String seedStr = String.valueOf(seed);
        // For long seed values, pad right instead of centering
        if (seedStr.length() >= W_SEED) {
            seedStr = seedStr.substring(0, W_SEED);
        } else {
            seedStr = seedStr + rep(' ', W_SEED - seedStr.length());
        }
        
        System.out.printf("| %-"+W_INST+"s| %-"+W_ALG+"s| %-"+W_SEED+
                          "s| %-"+W_BEST+"s| %-"+W_OPT+"s| %-"+W_TIME+"s|%n",
                inst,
                centre(alg,  W_ALG),
                seedStr,
                centre(String.valueOf(best), W_BEST),
                centre(String.valueOf(opt),  W_OPT),
                centre(String.format("%.4f", t), W_TIME));
    }

    private static void printRowSeparator() { System.out.println(hLine()); }

    // ── Wilcoxon ──────────────────────────────────────────────────────────

    private static void printWilcoxonSection(int[] ga, int[] ils) {
        System.out.println("=".repeat(78));
        System.out.println("  STATISTICAL ANALYSIS - One-Tailed Wilcoxon Signed-Rank Test (a = 0.05)");
        System.out.println("=".repeat(78));
        System.out.println("\n  H0 : Median difference between GA and ILS is zero.");
        System.out.println("  H1 : GA performance > ILS performance (one-tailed).\n");
        System.out.printf("  %-25s %-12s %-12s %-12s%n","Instance","GA","ILS","Difference");
        System.out.println("  " + "-".repeat(61));
        for (int i = 0; i < INSTANCES.length; i++) {
            String name = ((String) INSTANCES[i][1]).trim();
            System.out.printf("  %-25s %-12d %-12d %-12d%n",
                    name, ga[i], ils[i], ga[i]-ils[i]);
        }
        System.out.println();
        WilcoxonTest.WilcoxonResult r = WilcoxonTest.test(ga, ils);
        System.out.println("  Result: " + r.decision);
        System.out.println("\n" + "=".repeat(78));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static String centre(String s, int w) {
        if (s.length() >= w) return s.substring(0, w);
        int pad = w - s.length(), l = pad/2;
        return rep(' ', l) + s + rep(' ', pad - l);
    }

    private static String rep(char c, int n) {
        char[] a = new char[n]; java.util.Arrays.fill(a, c); return new String(a);
    }

    // ── Helper class for seed results ─────────────────────────────────────
    private static class SeedResult {
        long seed;
        int[] gaResults;
        int[] ilsResults;
        double totalTime;

        SeedResult(long seed, int[] gaResults, int[] ilsResults, double totalTime) {
            this.seed = seed;
            this.gaResults = gaResults;
            this.ilsResults = ilsResults;
            this.totalTime = totalTime;
        }
    }

    private static class DetailedSeedResult {
        long seed;
        int[] gaResults;
        int[] ilsResults;
        double[] gaTimes;
        double[] ilsTimes;

        DetailedSeedResult(long seed, int[] gaResults, int[] ilsResults, double[] gaTimes, double[] ilsTimes) {
            this.seed = seed;
            this.gaResults = gaResults;
            this.ilsResults = ilsResults;
            this.gaTimes = gaTimes;
            this.ilsTimes = ilsTimes;
        }
    }
}
