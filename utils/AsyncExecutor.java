package utils;

import core.AbstractWorkerTask;
import core.TaskConstants;
import interfaces.TaskStateListener;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

/**
 * High-performance, Priority-Aware Thread Pool Executor.
 * Demonstrates:
 * - ThreadPoolExecutor architecture with PriorityBlockingQueue
 * - Custom ComparableFutureTask for seamless Future/Priority integration
 * - Deterministic coordination via CountDownLatch (Zero arbitrary Thread.sleep)
 * - Safe lifecycle management (Graceful shutdown & awaitTermination)
 */
public class AsyncExecutor {

    private final CustomPriorityExecutorService executor;
    private final int corePoolSize;

    public AsyncExecutor() {
        this(3); // Default 3 concurrent worker threads
    }

    public AsyncExecutor(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        BlockingQueue<Runnable> priorityQueue = new PriorityBlockingQueue<>(16, new PriorityTaskComparator());
        this.executor = new CustomPriorityExecutorService(
                corePoolSize,
                corePoolSize,
                0L,
                TimeUnit.MILLISECONDS,
                priorityQueue,
                new DaemonThreadFactory("MASF-Worker")
        );
    }

    /**
     * Dispatches a single task asynchronously into the priority queue.
     */
    public Future<?> executeTask(AbstractWorkerTask task) {
        task.transitionState(TaskConstants.STATE_QUEUED);
        return executor.submit(task);
    }

    /**
     * Executes a batch of tasks concurrently and deterministically awaits completion
     * using a CountDownLatch. No arbitrary thread sleep is used.
     *
     * @param tasks List of tasks to execute
     * @param timeoutSeconds Maximum duration to await execution
     * @return true if all tasks completed within the timeout, false if timed out
     */
    public boolean executeAll(List<AbstractWorkerTask> tasks, long timeoutSeconds) {
        if (tasks == null || tasks.isEmpty()) {
            return true;
        }

        CountDownLatch latch = new CountDownLatch(tasks.size());

        TaskStateListener completionListener = (task, oldState, newState) -> {
            if (TaskConstants.STATE_COMPLETED.equals(newState) || TaskConstants.STATE_FAILED.equals(newState)) {
                latch.countDown();
            }
        };

        for (AbstractWorkerTask t : tasks) {
            t.addStateListener(completionListener);
            executeTask(t);
        }

        try {
            return latch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Gracefully shuts down the executor pool.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public int getActiveCount() {
        return executor.getActiveCount();
    }

    public long getCompletedTaskCount() {
        return executor.getCompletedTaskCount();
    }

    public int getQueueSize() {
        return executor.getQueue().size();
    }

    // =========================================================================
    // Concurrency Subclasses & Supporting Infrastructure
    // =========================================================================

    /**
     * Custom ThreadPoolExecutor overriding newTaskFor to preserve priority ordering
     * when tasks are wrapped into FutureTasks by executor.submit().
     */
    private static class CustomPriorityExecutorService extends ThreadPoolExecutor {
        public CustomPriorityExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                             TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                             ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
            return new PriorityFutureTask<>(runnable, value);
        }

        @Override
        protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            return super.newTaskFor(callable);
        }
    }

    /**
     * FutureTask implementation that delegates compareTo to the wrapped target task.
     */
    private static class PriorityFutureTask<T> extends FutureTask<T> implements Comparable<PriorityFutureTask<T>> {
        private final Object target;

        public PriorityFutureTask(Runnable runnable, T result) {
            super(runnable, result);
            this.target = runnable;
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public int compareTo(PriorityFutureTask<T> other) {
            if (this.target instanceof Comparable && other.target instanceof Comparable) {
                return ((Comparable) this.target).compareTo(other.target);
            }
            return 0;
        }
    }

    /**
     * Comparator for the PriorityBlockingQueue to compare any Runnables.
     */
    private static class PriorityTaskComparator implements Comparator<Runnable> {
        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public int compare(Runnable r1, Runnable r2) {
            if (r1 instanceof Comparable && r2 instanceof Comparable) {
                return ((Comparable) r1).compareTo(r2);
            }
            return 0;
        }
    }

    /**
     * Custom ThreadFactory providing meaningful thread names for logging.
     */
    private static class DaemonThreadFactory implements ThreadFactory {
        private final String prefix;
        private int counter = 1;

        public DaemonThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public synchronized Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + counter++);
            t.setDaemon(false);
            return t;
        }
    }
}
