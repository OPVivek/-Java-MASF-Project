package core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CPU-bound computational worker task.
 * Demonstrates: Polymorphism, method overriding, and measurable algorithmic workload.
 */
public class SimpleTask extends AbstractWorkerTask {
    private final int calculationLimit;

    public SimpleTask(String name) {
        this(name, 50000);
    }

    public SimpleTask(String name, int calculationLimit) {
        super(name);
        this.calculationLimit = Math.max(0, calculationLimit);
        this.setPriority(6); // Default normal-low priority
    }

    @Override
    protected TaskResult doWork() throws Exception {
        log("COMP", "Initiating mathematical compute cycle up to limit: " + calculationLimit);
        long start = System.currentTimeMillis();

        int primesFound = 0;
        for (int i = 2; i <= calculationLimit; i++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Compute task interrupted.");
            }
            if (isPrime(i)) {
                primesFound++;
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("limit", calculationLimit);
        metrics.put("primesCalculated", primesFound);
        metrics.put("durationMs", elapsed);

        log("COMP", String.format("Compute finished: %d primes identified in %dms", primesFound, elapsed));
        return TaskResult.success("Mathematical compute completed", elapsed, metrics);
    }

    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n <= 3) return true;
        if (n % 2 == 0 || n % 3 == 0) return false;
        for (int i = 5; i * i <= n; i += 6) {
            if (n % i == 0 || n % (i + 2) == 0) return false;
        }
        return true;
    }
}
