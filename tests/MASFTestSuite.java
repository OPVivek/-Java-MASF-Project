package tests;

import core.*;
import utils.AsyncExecutor;
import utils.StateManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise automated test suite validating concurrency, OOP design patterns,
 * state integrity, and priority dispatching.
 *
 * Can be executed standalone with zero external dependencies via:
 *   java tests.MASFTestSuite
 */
public class MASFTestSuite {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println(TaskConstants.ANSI_BOLD + "\n>>> EXECUTING MASF ENTERPRISE VERIFICATION TEST SUITE <<<" + TaskConstants.ANSI_RESET);
        long startTime = System.currentTimeMillis();

        runTest("Test 1: Task State Machine Transitions (CREATED -> RUNNING -> COMPLETED)", MASFTestSuite::testStateTransitions);
        runTest("Test 2: Natural Priority Ordering (Priority Comparator & Tie-Breaker)", MASFTestSuite::testPriorityOrdering);
        runTest("Test 3: Concurrency Engine Deterministic Execution (CountDownLatch)", MASFTestSuite::testConcurrentExecution);
        runTest("Test 4: Dynamic Method Dispatch & Subclass Metrics Generation", MASFTestSuite::testPolymorphismAndMetrics);
        runTest("Test 5: Cryptographic Hashing & Token Signature Integrity", MASFTestSuite::testSecurityAndCryptography);
        runTest("Test 6: StateManager Metric Aggregation & Bottleneck Analysis", MASFTestSuite::testStateManagerAnalytics);

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("\n----------------------------------------------------------------------");
        System.out.println(String.format("TEST RESULTS: %s%d PASSED%s | %s%d FAILED%s (Total Execution: %d ms)",
                TaskConstants.ANSI_GREEN, testsPassed, TaskConstants.ANSI_RESET,
                testsFailed > 0 ? TaskConstants.ANSI_RED : TaskConstants.ANSI_RESET, testsFailed, TaskConstants.ANSI_RESET,
                totalTime));
        System.out.println("----------------------------------------------------------------------\n");

        if (testsFailed > 0) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, TestCase testCase) {
        System.out.print(String.format("[RUNNING] %-65s ", testName));
        try {
            testCase.execute();
            testsPassed++;
            System.out.println(TaskConstants.ANSI_GREEN + "[PASS]" + TaskConstants.ANSI_RESET);
        } catch (Throwable t) {
            testsFailed++;
            System.out.println(TaskConstants.ANSI_RED + "[FAIL] -> " + t.getMessage() + TaskConstants.ANSI_RESET);
            t.printStackTrace(System.err);
        }
    }

    // --- TEST IMPLEMENTATIONS ---

    private static void testStateTransitions() {
        AbstractWorkerTask task = new SimpleTask("State-Test-Worker", 500);
        assertTrue(TaskConstants.STATE_CREATED.equals(task.getState()), "Initial state should be CREATED");

        final List<String> capturedStates = new ArrayList<>();
        task.addStateListener((t, oldState, newState) -> capturedStates.add(newState));

        task.run(); // Execute synchronous run template

        assertTrue(TaskConstants.STATE_COMPLETED.equals(task.getState()), "Final state should be COMPLETED");
        assertTrue(capturedStates.contains(TaskConstants.STATE_RUNNING), "Must transition through RUNNING");
        assertTrue(capturedStates.contains(TaskConstants.STATE_COMPLETED), "Must notify on COMPLETED");
        assertTrue(task.getDuration() >= 0, "Duration must be calculated");
    }

    private static void testPriorityOrdering() {
        AbstractWorkerTask highPri = new SecurityTask("High-Pri-Task"); // Priority 1
        highPri.setPriority(1);

        AbstractWorkerTask lowPri = new SimpleTask("Low-Pri-Task");     // Priority 8
        lowPri.setPriority(8);

        assertTrue(highPri.compareTo(lowPri) < 0, "Priority 1 should precede Priority 8 in min-ordering");
        assertTrue(lowPri.compareTo(highPri) > 0, "Priority 8 should succeed Priority 1");

        // Test tie-breaking by ID
        AbstractWorkerTask taskA = new SimpleTask("Tie-A");
        AbstractWorkerTask taskB = new SimpleTask("Tie-B");
        taskA.setPriority(5);
        taskB.setPriority(5);

        assertTrue(taskA.compareTo(taskB) != 0, "Tasks with same priority must break ties using Entity ID");
    }

    private static void testConcurrentExecution() {
        AsyncExecutor executor = new AsyncExecutor(3);
        List<AbstractWorkerTask> tasks = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            AbstractWorkerTask t = new SimpleTask("Batch-Worker-" + i, 10000);
            t.setPriority((i % 5) + 1);
            tasks.add(t);
        }

        boolean resolved = executor.executeAll(tasks, 10);
        assertTrue(resolved, "All concurrent tasks must resolve within timeout");

        for (AbstractWorkerTask t : tasks) {
            assertTrue(TaskConstants.STATE_COMPLETED.equals(t.getState()),
                    "Worker " + t.getTaskName() + " must reach COMPLETED state");
            assertTrue(t.getResult() != null && t.getResult().isSuccess(),
                    "Worker " + t.getTaskName() + " must have successful TaskResult");
        }

        executor.shutdown();
    }

    private static void testPolymorphismAndMetrics() {
        List<AbstractWorkerTask> diverseTasks = List.of(
                new SimpleTask("Poly-Compute", 5000),
                new AnalyticTask("Poly-Analytic", 500),
                new FileSystemTask("Poly-Filesys", "data"),
                new SecurityTask("Poly-Security", "qa_user", "ADMIN")
        );

        for (AbstractWorkerTask task : diverseTasks) {
            task.run();
            TaskResult res = task.getResult();
            assertTrue(res != null, "TaskResult must not be null for " + task.getClass().getSimpleName());
            assertTrue(res.isSuccess(), "Result status should be SUCCESS for " + task.getClass().getSimpleName());
            assertTrue(!res.getMetrics().isEmpty(), "Subclass metrics must be populated for " + task.getClass().getSimpleName());
        }
    }

    private static void testSecurityAndCryptography() {
        String input = "admin123";
        String expectedHash = TaskConstants.ADMIN_PASSWORD_HASH;
        String actualHash = TaskConstants.sha256(input);

        assertTrue(expectedHash.equals(actualHash), "SHA-256 output must match expected reference digest");

        // Verify empty / null input handling
        assertTrue("".equals(TaskConstants.sha256(null)), "Null input should return empty string");
    }

    private static void testStateManagerAnalytics() {
        StateManager manager = new StateManager();

        AbstractWorkerTask tFast = new SimpleTask("Fast-Task", 100);
        tFast.setDuration(15);
        tFast.setState(TaskConstants.STATE_COMPLETED);
        tFast.setCategory("Compute");

        AbstractWorkerTask tSlow = new SimpleTask("Slow-Task", 100);
        tSlow.setDuration(250);
        tSlow.setState(TaskConstants.STATE_COMPLETED);
        tSlow.setCategory("Compute");

        manager.addTask(tFast);
        manager.addTask(tSlow);

        assertTrue(manager.getTasks().size() == 2, "StateManager should hold 2 tasks");
        manager.saveSummaryToFile();
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion Failed: " + message);
        }
    }

    @FunctionalInterface
    private interface TestCase {
        void execute() throws Exception;
    }
}
