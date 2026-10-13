package core;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encapsulates the execution outcome, performance metrics, and computational payload of a task.
 */
public class TaskResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        SUCCESS,
        FAILED,
        INTERRUPTED
    }

    private final Status status;
    private final String message;
    private final long executionDurationMs;
    private final Map<String, Object> metrics;
    private final Throwable error;

    public TaskResult(Status status, String message, long executionDurationMs) {
        this(status, message, executionDurationMs, Collections.emptyMap(), null);
    }

    public TaskResult(Status status, String message, long executionDurationMs, Map<String, Object> metrics) {
        this(status, message, executionDurationMs, metrics, null);
    }

    public TaskResult(Status status, String message, long executionDurationMs, Map<String, Object> metrics, Throwable error) {
        this.status = status;
        this.message = message;
        this.executionDurationMs = executionDurationMs;
        this.metrics = (metrics == null) ? Collections.emptyMap() : new LinkedHashMap<>(metrics);
        this.error = error;
    }

    public static TaskResult success(String message, long durationMs, Map<String, Object> metrics) {
        return new TaskResult(Status.SUCCESS, message, durationMs, metrics, null);
    }

    public static TaskResult failure(String message, long durationMs, Throwable error) {
        return new TaskResult(Status.FAILED, message, durationMs, Collections.emptyMap(), error);
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public String getMessage() {
        return message;
    }

    public long getExecutionDurationMs() {
        return executionDurationMs;
    }

    public Map<String, Object> getMetrics() {
        return Collections.unmodifiableMap(metrics);
    }

    public Throwable getError() {
        return error;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(status).append("] ").append(message)
          .append(" (").append(executionDurationMs).append("ms)");
        if (!metrics.isEmpty()) {
            sb.append(" | Metrics: ").append(metrics);
        }
        if (error != null) {
            sb.append(" | Error: ").append(error.getMessage());
        }
        return sb.toString();
    }
}
