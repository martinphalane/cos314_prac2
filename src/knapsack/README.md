# COS314 Assignment 2 – 0/1 Knapsack Solver
## Genetic Algorithm (GA) vs Iterated Local Search (ILS)

---

## Requirements

- Java 11 or later (tested on OpenJDK 21)
- No external libraries required

---

## How to Run

Place instance files in a `data/` subfolder next to the JAR, then:

```
java -jar KnapsackSolver.jar
```

The program prompts interactively:

```
=== Knapsack Problem Solver ===
1. Use single seed (user input)
2. Generate random seeds (at least 30 iterations)

Enter your choice (1 or 2):
Enter custom file path (press Enter to use default):
```

### Mode 1 – Single Seed
Runs both GA and ILS on all instances with one user-supplied seed.
Produces the comparison table and Wilcoxon test output.

```
Choice: 1
Custom path: <Enter>
Seed: 42
```

### Mode 2 – Multiple Seeds (30+)
Runs both algorithms over N random seeds (min 30), shows per-seed
average results, then displays the full detail table for the best seed.

```
Choice: 2
Custom path: <Enter>
Iterations: 30
```

---

## Data Folder Layout

```
data/
  f1_l-d_kp_10_269
  f2_l-d_kp_20_878
  f3_l-d_kp_4_20
  f4_l-d_kp_4_11
  f5_l-d_kp_15_375
  f6_l-d_kp_10_60
  f7_l-d_kp_7_50
  f8_l-d_kp_23_10000
  f9_l-d_kp_5_80
  f10_l-d_kp_20_879
  knapPI_1_100_1000_1
KnapsackSolver.jar
README.md
```

---

## Algorithm Configuration

### Genetic Algorithm (GA)
Parameters scale adaptively with instance size n:

| Parameter | Value |
|-----------|-------|
| Population size | min(max(100, 5n), 200) |
| Max generations | min(max(500, 20n), 2000) |
| Crossover rate | 0.85 (single-point) |
| Mutation rate | 1/n per gene (avg 1 flip) |
| Selection | Binary tournament (k=2) |
| Elitism | Top-1 preserved each generation |
| Initialisation | 20% greedy-seeded + 80% random |
| Repair | Remove lowest v/w items until feasible |

### Iterated Local Search (ILS)
| Parameter | Value |
|-----------|-------|
| Max iterations | min(max(1000, 20n), 3000) |
| Perturbation strength | max(4, n/10) bits flipped |
| Local search | Best-improvement 1-bit flip |
| Initial solution | Greedy (descending v/w ratio) |
| Acceptance | Strict improvement only |
| Repair | Remove lowest v/w items until feasible |

---

## Known Optimums (DP-verified for provided integer-rounded instances)

| Instance | Known Optimum |
|----------|--------------|
| f1_l-d_kp_10_269 | 431 |
| f2_l-d_kp_20_878 | 1042 |
| f3_l-d_kp_4_20 | 11 |
| f4_l-d_kp_4_11 | 4 |
| f5_l-d_kp_15_375 | 648 |
| f6_l-d_kp_10_60 | 80 |
| f7_l-d_kp_7_50 | 26 |
| f8_l-d_kp_23_10000 | 9777 |
| f9_l-d_kp_5_80 | 68 |
| f10_l-d_kp_20_879 | 1042 |
| knapPI_1_100_1000_1 | 6384 |

Note: f5 has decimal weights/values in the source file; InstanceLoader rounds
to nearest integer, yielding a different (higher) optimum than the xlsx value.

---

## Statistical Test

One-tailed Wilcoxon signed-rank test (alpha = 0.05).
H0: median difference between GA and ILS is zero.
H1: GA > ILS (one-tailed).
Critical values from the standard Wilcoxon table (n <= 25),
normal approximation (z < -1.645) for n > 25.

---

## Project Structure

```
src/knapsack/
  Main.java                – Entry point, table output, Wilcoxon section
  GeneticAlgorithm.java    – Population-based metaheuristic
  IteratedLocalSearch.java – Trajectory-based metaheuristic
  KnapsackInstance.java    – Problem model (evaluate, totalWeight, etc.)
  InstanceLoader.java      – Reads instance files (int and float weights)
  WilcoxonTest.java        – One-tailed Wilcoxon signed-rank test
KnapsackSolver.jar
README.md
```
