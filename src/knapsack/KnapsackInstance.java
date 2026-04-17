package knapsack;

public class KnapsackInstance {
    private final String name;
    private final int    capacity;
    private final int[]  weights;
    private final int[]  values;
    private final int    n;

    public KnapsackInstance(String name, int capacity, int[] weights, int[] values) {
        if (weights.length != values.length)
            throw new IllegalArgumentException("weights and values arrays must have the same length");
        this.name     = name;
        this.capacity = capacity;
        this.weights  = weights.clone();
        this.values   = values.clone();
        this.n        = weights.length;
    }

    public String getName()        { return name;       }
    public int    getCapacity()    { return capacity;   }
    public int    getNumItems()    { return n;          }
    public int    getWeight(int i) { return weights[i]; }
    public int    getValue(int i)  { return values[i];  }
    public int[]  getWeights()     { return weights.clone(); }
    public int[]  getValues()      { return values.clone();  }

    public int evaluate(boolean[] chromosome) {
        int totalWeight = 0, totalValue = 0;
        for (int i = 0; i < n; i++) {
            if (chromosome[i]) {
                totalWeight += weights[i];
                totalValue  += values[i];
            }
        }
        return (totalWeight <= capacity) ? totalValue : 0;
    }

    public int totalWeight(boolean[] chromosome) {
        int w = 0;
        for (int i = 0; i < n; i++) if (chromosome[i]) w += weights[i];
        return w;
    }

    public int totalValue(boolean[] chromosome) {
        int v = 0;
        for (int i = 0; i < n; i++) if (chromosome[i]) v += values[i];
        return v;
    }
}
