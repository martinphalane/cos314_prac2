package knapsack;

import java.io.File;
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
        System.out.print("Enter random seed value: ");
        long seed = scanner.nextLong();
        scanner.close();

        System.out.println("\nSeed: " + seed);
        System.out.println("Running algorithms on 10 knapsack instances...\n");

        int[] gaResults  = new int[INSTANCES.length];
        int[] ilsResults = new int[INSTANCES.length];

        printTableHeader();

        for (int inst = 0; inst < INSTANCES.length; inst++) {
            String fileName    = (String)  INSTANCES[inst][0];
            String displayName = (String)  INSTANCES[inst][1];
            int    knownOpt    = (Integer) INSTANCES[inst][2];

            String filePath = DATA_DIR + fileName;

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

    // ── Table formatting ──────────────────────────────────────────────────

    private static final int W_INST = 22, W_ALG = 11, W_SEED = 12,
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

    private static void printRow(String inst, String alg, long seed,
                                  int best, int opt, double t) {
        System.out.printf("| %-"+W_INST+"s| %-"+W_ALG+"s| %-"+W_SEED+
                          "s| %-"+W_BEST+"s| %-"+W_OPT+"s| %-"+W_TIME+"s|%n",
                inst,
                centre(alg,  W_ALG),
                centre(String.valueOf(seed), W_SEED),
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
}
