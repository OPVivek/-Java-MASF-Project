package core;

import interfaces.Loggable;
import interfaces.Prioritizable;
import interfaces.TaskStateListener;
import utils.FileService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract Worker Task defining the strict template lifecycle for all executable tasks.
 * Demonstrates:
 * - Multilevel Inheritance: BaseEntity -> BaseTask -> AbstractWorkerTask
 * - Multiple Inheritance via Interfaces: Runnable, Loggable, Prioritizable, Comparable
 * - Template Method Pattern: 'run()' is final and defines the invariant execution algorithm
 * - Observer Pattern: TaskStateListener notifications
 * - Method Overloading: log(String) and log(String, String)
 */
public abstract class AbstractWorkerTask extends BaseTask 
        implements Runnable, Loggable, Prioritizable, Comparable<AbstractWorkerTask> {

    private int priority; // 1 (Highest) to 10 (Lowest)
    private TaskResult result;
    private final List<TaskStateListener> listeners = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public AbstractWorkerTask(String name) {
        super(name);
        this.priority = 5; // Default normal priority
    }

    /**
     * Primitive operation that each specialized task must implement.
     * Demonstrates dynamic method dispatch and polymorphism.
     */
    protected abstract TaskResult doWork() throws Exception;

    /**
     * Template Method enforcing the lifecycle algorithm.
     * Declared final to prevent subclasses from altering the security and execution guarantees.
     */
    @Override
    public final void run() {
        this.setStartTime(System.currentTimeMillis());
        transitionState(TaskConstants.STATE_RUNNING);
        log("INIT", "Starting task execution on thread: " + Thread.currentThread().getName());

        try {
            beforeExecute();
            this.result = doWork();
            if (this.result == null) {
                this.result = TaskResult.success("Task completed successfully", 0, null);
            }
            transitionState(TaskConstants.STATE_COMPLETED);
            log("DONE", "Task completed successfully. Result: " + this.result.getMessage());
        } catch (InterruptedException e) {
            this.result = TaskResult.failure("Task interrupted: " + e.getMessage(), 0, e);
            transitionState(TaskConstants.STATE_FAILED);
            log("WARN", "Task interrupted during execution: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            this.result = TaskResult.failure("Task execution error: " + e.getMessage(), 0, e);
            transitionState(TaskConstants.STATE_FAILED);
            log("FAIL", "Task execution error: " + e.getMessage());
        } finally {
            this.setEndTime(System.currentTimeMillis());
            afterExecute(this.result);
            cleanup();
        }
    }

    /**
     * Lifecycle hook executed right before doWork().
     */
    protected void beforeExecute() {
        // Optional override for subclasses
    }

    /**
     * Lifecycle hook executed right after execution concludes.
     */
    protected void afterExecute(TaskResult executionResult) {
        // Optional override for subclasses
    }

    /**
     * Simulates destructor / resource disposal for task artifacts.
     */
    public void cleanup() {
        // Subclasses may release file handles, sockets, etc.
    }

    /**
     * Thread-safe state transition with observer notification.
     */
    public void transitionState(String newState) {
        String oldState = getState();
        setState(newState);
        for (TaskStateListener listener : listeners) {
            try {
                listener.onStateChanged(this, oldState, newState);
            } catch (Exception e) {
                // Prevent listener errors from aborting task flow
            }
        }
    }

    public void addStateListener(TaskStateListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeStateListener(TaskStateListener listener) {
        listeners.remove(listener);
    }

    public TaskResult getResult() {
        return result;
    }

    @Override
    public synchronized void setPriority(int priority) {
        if (priority < 1) priority = 1;
        if (priority > 10) priority = 10;
        this.priority = priority;
    }

    @Override
    public synchronized int getPriority() {
        return priority;
    }

    /**
     * Natural ordering for PriorityBlockingQueue:
     * Lower numeric value represents HIGHER priority (1 is highest, 10 is lowest).
     * Tie-breaker: Task ID (creation order FIFO).
     */
    @Override
    public int compareTo(AbstractWorkerTask other) {
        if (other == null) return -1;
        int pCompare = Integer.compare(this.priority, other.priority);
        if (pCompare != 0) {
            return pCompare;
        }
        return Long.compare(this.getId(), other.getId());
    }

    // Overloaded log method 1
    @Override
    public void log(String message) {
        this.log("INFO", message);
    }

    // Overloaded log method 2
    @Override
    public void log(String level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String logEntry = String.format("[%s] [%-5s] [Pri:%02d] [%s] %s",
                timestamp, level, getPriority(), getTaskName(), message);
        System.out.println(logEntry);
        FileService.appendToFile(TaskConstants.LOG_FILE, logEntry);
    }

    public static DateTimeFormatter getFormatter() {
        return FORMATTER;
    }
}
