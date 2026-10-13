package core;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Analytical worker processing statistical aggregations and data distributions.
 * Demonstrates: Statistical data processing, sorting, and analytical metric aggregation.
 */
public class AnalyticTask extends AbstractWorkerTask {
    private final int sampleSize;

    public AnalyticTask(String name) {
        this(name, 2500);
    }

    public AnalyticTask(String name, int sampleSize) {
        super(name);
        this.sampleSize = Math.max(1, sampleSize);
        this.setPriority(4); // Medium priority
    }

    @Override
    protected TaskResult doWork() throws Exception {
        log("STATS", "Synthesizing dataset with " + sampleSize + " telemetry records...");
        long start = System.currentTimeMillis();

        double[] dataset = new double[sampleSize];
        Random rand = new Random(42);
        double sum = 0.0;
        for (int i = 0; i < sampleSize; i++) {
            dataset[i] = 10.0 + (rand.nextGaussian() * 5.0);
            sum += dataset[i];
        }

        // Statistical processing
        Arrays.sort(dataset);
        double mean = sum / sampleSize;
        double median = (sampleSize % 2 == 0) 
                ? (dataset[sampleSize / 2 - 1] + dataset[sampleSize / 2]) / 2.0 
                : dataset[sampleSize / 2];

        double varianceSum = 0.0;
        for (double v : dataset) {
            varianceSum += Math.pow(v - mean, 2);
        }
        double stdDev = Math.sqrt(varianceSum / sampleSize);
        double min = dataset[0];
        double max = dataset[sampleSize - 1];
        double p95 = dataset[(int) (sampleSize * 0.95)];

        // Small processing delay to simulate heavy multi-variable aggregation
        Thread.sleep(150);

        long elapsed = System.currentTimeMillis() - start;
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("sampleSize", sampleSize);
        metrics.put("mean", String.format("%.2f", mean));
        metrics.put("median", String.format("%.2f", median));
        metrics.put("stdDev", String.format("%.2f", stdDev));
        metrics.put("min", String.format("%.2f", min));
        metrics.put("max", String.format("%.2f", max));
        metrics.put("p95", String.format("%.2f", p95));
        metrics.put("computeDurationMs", elapsed);

        log("STATS", String.format("Aggregation finished: Mean=%.2f | Median=%.2f | StdDev=%.2f | P95=%.2f",
                mean, median, stdDev, p95));

        return TaskResult.success("Statistical data aggregation completed", elapsed, metrics);
    }
}
