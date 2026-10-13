package core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Network I/O worker simulating distributed API synchronization.
 * Demonstrates: Concurrency handling, retry simulation, and I/O latency modeling.
 */
public class NetworkTask extends AbstractWorkerTask {
    private final String targetEndpoint;
    private final int payloadSizeKb;
    private static final Random RANDOM = new Random();

    public NetworkTask(String name) {
        this(name, "https://api.salesforce.masf/v1/sync", 128);
    }

    public NetworkTask(String name, String targetEndpoint, int payloadSizeKb) {
        super(name);
        this.targetEndpoint = (targetEndpoint == null || targetEndpoint.trim().isEmpty()) 
                ? "https://api.salesforce.masf/v1/default" : targetEndpoint;
        this.payloadSizeKb = Math.max(0, payloadSizeKb);
        this.setPriority(2); // High priority (external I/O dependency)
    }

    @Override
    protected TaskResult doWork() throws Exception {
        log("NET", "Establishing TLS handshake with endpoint: " + targetEndpoint);
        long start = System.currentTimeMillis();

        // Simulate network latency & DNS resolution
        int latency = 120 + RANDOM.nextInt(150);
        Thread.sleep(latency);

        log("NET", String.format("Connected. Streaming payload of %d KB...", payloadSizeKb));
        int transferTime = (payloadSizeKb * 2) + RANDOM.nextInt(50);
        Thread.sleep(transferTime);

        long totalElapsed = System.currentTimeMillis() - start;
        int httpStatusCode = 200;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("endpoint", targetEndpoint);
        metrics.put("httpStatus", httpStatusCode);
        metrics.put("payloadKb", payloadSizeKb);
        metrics.put("latencyMs", latency);
        metrics.put("totalTransferTimeMs", totalElapsed);

        log("NET", String.format("Data transfer OK (Status %d, Transferred %d KB in %dms)",
                httpStatusCode, payloadSizeKb, totalElapsed));

        return TaskResult.success("Network sync succeeded with HTTP " + httpStatusCode, totalElapsed, metrics);
    }
}
