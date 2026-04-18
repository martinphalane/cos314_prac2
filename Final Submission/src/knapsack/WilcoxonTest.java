package knapsack;

import java.util.Arrays;

/**
 * =============================================================================
 *  One-Tailed Wilcoxon Signed-Rank Test
 * =============================================================================
 *
 * Implements the Wilcoxon signed-rank test as required by the assignment.
 *
 * The test is applied to paired observations (GA result, ILS result) on the
 * same 10 problem instances.
 *
 * H0 (null hypothesis):     The median difference between GA and ILS
 *                           performance is zero (means are equivalent).
 * H1 (alternative):         GA performance > ILS performance  (one-tailed,
 *                           i.e. we test whether GA is significantly better).
 *
 * Significance level: α = 0.05 (5%)
 *
 * Critical value for n = 10 paired samples, one-tailed α = 0.05:
 *   W_critical = 10  (from standard Wilcoxon table)
 *   Reject H0 if W+ ≤ W_critical  (where W+ = sum of positive ranks)
 *
 * References:
 *   Wilcoxon, F. (1945). Individual comparisons by ranking methods.
 *   Biometrics Bulletin, 1(6), 80-83.
 * =============================================================================
 */
public class WilcoxonTest {

    /**
     * Performs the one-tailed Wilcoxon signed-rank test.
     *
     * @param gaResults  array of GA best-solution values (one per instance)
     * @param ilsResults array of ILS best-solution values (one per instance)
     * @return a {@link WilcoxonResult} containing W+, W-, and the decision
     */
    public static WilcoxonResult test(int[] gaResults, int[] ilsResults) {

        int n = gaResults.length;
        if (n != ilsResults.length) {
            throw new IllegalArgumentException(
                "GA and ILS result arrays must have equal length");
        }

        /* Step 1 – Compute pairwise differences d_i = GA_i - ILS_i */
        double[] differences = new double[n];
        for (int i = 0; i < n; i++) {
            differences[i] = gaResults[i] - ilsResults[i];
        }

        /* Step 2 – Exclude zero differences; collect |d_i| for non-zeros */
        int effectiveN = 0;
        for (double d : differences) {
            if (d != 0) effectiveN++;
        }

        if (effectiveN == 0) {
            /* All differences are zero – no evidence against H0 */
            return new WilcoxonResult(0, 0, effectiveN,
                    false, "All differences are zero; cannot reject H0.");
        }

        /* Gather non-zero (difference, sign) pairs */
        double[] absDiffs = new double[effectiveN];
        int[]    signs    = new int[effectiveN];
        int idx = 0;
        for (double d : differences) {
            if (d != 0) {
                absDiffs[idx] = Math.abs(d);
                signs[idx]    = (d > 0) ? 1 : -1;
                idx++;
            }
        }

        /* Step 3 – Rank |d_i| in ascending order (handle ties by average rank) */
        Integer[] order = new Integer[effectiveN];
        for (int i = 0; i < effectiveN; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(absDiffs[a], absDiffs[b]));

        double[] ranks = new double[effectiveN];
        int i = 0;
        while (i < effectiveN) {
            int j = i;
            /* Find all ties */
            while (j < effectiveN - 1
                   && absDiffs[order[j]] == absDiffs[order[j + 1]]) {
                j++;
            }
            double avgRank = (i + 1 + j + 1) / 2.0;   // 1-based average
            for (int k = i; k <= j; k++) {
                ranks[order[k]] = avgRank;
            }
            i = j + 1;
        }

        /* Step 4 – Compute W+ (sum of ranks where d_i > 0)
         *          and W- (sum of ranks where d_i < 0)  */
        double wPlus  = 0;
        double wMinus = 0;
        for (int k = 0; k < effectiveN; k++) {
            if (signs[k] > 0) wPlus  += ranks[k];
            else               wMinus += ranks[k];
        }

        /*
         * Step 5 – One-tailed test: H1: GA > ILS
         *   The test statistic is the SMALLER of W+ and W-.
         *   W_critical for n=10, α=0.05, one-tailed = 10
         *   We reject H0 if W (min of W+, W-) ≤ W_critical
         *   AND the direction is consistent with H1 (i.e. W+ < W-, meaning
         *   GA tends to be larger).
         *
         *   For general n we use a normal approximation when n > 10.
         */
        double wStat         = Math.min(wPlus, wMinus);
        boolean rejectH0;
        String  decision;

        if (effectiveN <= 25) {
            /* Use exact critical value table (one-tailed a=0.05) */
            int wCritical = exactCriticalValue(effectiveN);
            if (wCritical < 0) {
                /* Not enough non-zero differences to apply the test */
                rejectH0 = false;
                decision = String.format(
                    "W+ = %.1f, W- = %.1f. effectiveN = %d is too small for the " +
                    "Wilcoxon table (need n >= 5). FAIL TO REJECT H0: insufficient " +
                    "data to detect a significant difference at a=0.05.",
                    wPlus, wMinus, effectiveN);
            } else {
                rejectH0 = (wStat <= wCritical);
                decision = String.format(
                    "W+ = %.1f, W- = %.1f, W_stat = %.1f, W_critical(%d, a=0.05) = %d. %s",
                    wPlus, wMinus, wStat, effectiveN, wCritical,
                    rejectH0 ? "REJECT H0: GA is significantly better than ILS."
                              : "FAIL TO REJECT H0: no significant difference at a=0.05.");
            }
        } else {
            /* Normal approximation */
            double mu    = (double) effectiveN * (effectiveN + 1) / 4.0;
            double sigma = Math.sqrt((double) effectiveN
                                    * (effectiveN + 1)
                                    * (2 * effectiveN + 1) / 24.0);
            double z     = (wStat - mu) / sigma;
            double zCrit = -1.645;  // z for one-tailed a=0.05
            rejectH0 = (z < zCrit);
            decision = String.format(
                "W+ = %.1f, W- = %.1f, z = %.4f, z_critical = %.3f. %s",
                wPlus, wMinus, z, zCrit,
                rejectH0 ? "REJECT H0: GA is significantly better than ILS."
                          : "FAIL TO REJECT H0: no significant difference at a=0.05.");
        }

        return new WilcoxonResult(wPlus, wMinus, effectiveN, rejectH0, decision);
    }

    /**
     * Returns the one-tailed Wilcoxon critical value W for n pairs at α=0.05.
     * Source: standard Wilcoxon signed-rank table.
     */
    private static int exactCriticalValue(int n) {
        /* Table: n -> W_critical (one-tailed α = 0.05) */
        int[] table = {
        //  n= 0  1  2  3  4  5  6  7  8   9  10  11  12  13  14  15
            -1, -1, -1, -1, -1, 0,  2,  3,  5,  8, 10, 13, 17, 21, 25, 30,
        //  n=16  17  18  19  20  21  22  23  24  25
            35, 41, 47, 53, 60, 67, 75, 83, 91, 100
        };
        if (n < 0 || n >= table.length) return -1;
        return table[n];
    }

    /* ------------------------------------------------------------------ */
    /*  Result record                                                       */
    /* ------------------------------------------------------------------ */

    public static class WilcoxonResult {
        public final double  wPlus;
        public final double  wMinus;
        public final int     effectiveN;
        public final boolean rejectH0;
        public final String  decision;

        WilcoxonResult(double wPlus, double wMinus,
                       int effectiveN, boolean rejectH0,
                       String decision) {
            this.wPlus      = wPlus;
            this.wMinus     = wMinus;
            this.effectiveN = effectiveN;
            this.rejectH0   = rejectH0;
            this.decision   = decision;
        }
    }
}
