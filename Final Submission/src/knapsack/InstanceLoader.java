package knapsack;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a knapsack problem instance from a plain-text file.
 *
 * File format (as supplied with the assignment):
 * -----------------------------------------------
 * Line 1:  <n>  <W>          (number of items, then capacity)
 * Lines 2…n+1: <weight_i>  <value_i>   (one item per line)
 *
 * Blank lines and lines starting with '#' are ignored.
 */
public class InstanceLoader {

    /**
     * Loads a KnapsackInstance from a file.
     *
     * @param filePath  absolute or relative path to the instance file
     * @param instName  name to assign to the instance (e.g. "f1_l_d_kp_10_269")
     * @return          the loaded KnapsackInstance
     * @throws IOException if the file cannot be read or is malformed
     */
    public static KnapsackInstance load(String filePath,
                                        String instName) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            /* ---- read header ---- */
            String headerLine = nextDataLine(br);
            if (headerLine == null) {
                throw new IOException("Empty file: " + filePath);
            }
            String[] headerParts = headerLine.trim().split("\\s+");
            if (headerParts.length < 2) {
                throw new IOException("Malformed header in: " + filePath);
            }
            int n        = Integer.parseInt(headerParts[0]);
            int capacity = Integer.parseInt(headerParts[1]);

            /* ---- read items ---- */
            int[] weights = new int[n];
            int[] values  = new int[n];

            for (int i = 0; i < n; i++) {
                String line = nextDataLine(br);
                if (line == null) {
                    throw new IOException(
                        "Expected " + n + " items but found only " + i +
                        " in: " + filePath);
                }
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 2) {
                    throw new IOException(
                        "Malformed item line " + (i + 2) + " in: " + filePath);
                }
                // Parse as double first (handles both integer and decimal values), then convert to int
                weights[i] = (int) Math.round(Double.parseDouble(parts[0]));
                values[i]  = (int) Math.round(Double.parseDouble(parts[1]));
            }

            return new KnapsackInstance(instName, capacity, weights, values);
        }
    }

    /** Returns the next non-blank, non-comment line, or null at EOF. */
    private static String nextDataLine(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                return line;
            }
        }
        return null;
    }
}
