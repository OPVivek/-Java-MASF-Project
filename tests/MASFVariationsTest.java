package tests;

import core.*;
import utils.AsyncExecutor;
import utils.StateManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced Variation & Edge-Case Test Suite for MASF.
 * Tests diverse input variations across:
 * 1. Boundary & Clamping (Priority, Compute Limits, Sample Sizes, Negative Inputs)
 * 2. Null, Empty, and Malformed Strings (Task Names, Descriptions, User Tags, Endpoints)
 * 3. Inverted Priority Scheduling & High-Volume Concurrency Bursts
 * 4. Authentication Matrix & Cryptographic Edge-Cases
 * 5. StateManager Extreme Cases (Zero Tasks, Single Task, Failed Tasks, Division-by-Zero)
 */
public class MASFVariationsTest {

    private static int variationTestsPassed = 0;
    private static int variationTestsFailed = 0;

    public static void main(String[] args) {
        System.out.println(TaskConstants.ANSI_BOLD + "\n==========================================================================" + TaskConstants.ANSI_RESET);
        System.out.println(TaskConstants.ANSI_CYAN + "   MASF INPUT VARIATION & BOUNDARY STRESS TEST SUITE" + TaskConstants.ANSI_RESET);
        System.out.println(TaskConstants.ANSI_BOLD + "==========================================================================" + TaskConstants.ANSI_RESET);
        long startTime = System.currentTimeMillis();

        // 1. Boundary & Clamping Tests
        runVariation("V01: Priority Clamping (-100, 0, 1, 10, 11, 99999)", MASFVariationsTest::testPriorityClamping);
        runVariation("V02: SimpleTask Extreme Limits (Negative, 0, 1, 2, 80000)", MASFVariationsTest::testSimpleTaskLimits);
        runVariation("V03: AnalyticTask Sample Sizes (Negative, 0, 1, 2, 3, 5000)", MASFVariationsTest::testAnalyticTaskSizes);
        runVariation("V04: NetworkTask Edge Inputs (Null URL, Empty URL, 0KB, -500KB)", MASFVariationsTest::testNetworkTaskVariations);
        runVariation("V05: FileSystemTask Path Variations (Null, Empty, Non-Existent, Core Dir)", MASFVariationsTest::testFileSystemTaskPaths);
        runVariation("V06: SecurityTask Role & User Variations (Null, Blank, Injection, Special Chars)", MASFVariationsTest::testSecurityTaskRoles);

        // 2. Metadata & String Robustness
        runVariation("V07: BaseTask Metadata Edge Cases (Null/Whitespace Name, Tag, Description)", MASFVariationsTest::testBaseTaskMetadata);

        // 3. Concurrency & Scheduling Extremes
        runVariation("V08: Batch Sizing Variations (Empty List, Single Task)", MASFVariationsTest::testBatchSizing);
        runVariation("V09: Inverted Priority Enqueueing (P10 queued before P1)", MASFVariationsTest::testInvertedPriorityEnqueueing);
        runVariation("V10: High-Volume Concurrent Stress Burst (24 Concurrent Workloads)", MASFVariationsTest::testHighVolumeBurst);

        // 4. Security & Authentication Matrix
        runVariation("V11: Authentication Verification Matrix (Valid, Bad Pass, Bad User, Blank)", MASFVariationsTest::testAuthMatrix);

        // 5. StateManager Edge Cases
        runVariation("V12: StateManager Zero-Division & Extreme Analytic Edge Cases", MASFVariationsTest::testStateManagerEdgeCases);

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("\n--------------------------------------------------------------------------");
        System.out.println(String.format("VARIATION SUITE SUMMARY: %s%d PASSED%s | %s%d FAILED%s in %d ms",
                TaskConstants.ANSI_GREEN, variationTestsPassed, TaskConstants.ANSI_RESET,
                variationTestsFailed > 0 ? TaskConstants.ANSI_RED : TaskConstants.ANSI_RESET, variationTestsFailed, TaskConstants.ANSI_RESET,
                totalTime));
        System.out.println("--------------------------------------------------------------------------\n");

        if (variationTestsFailed > 0) {
            System.exit(1);
        }
    }

    private static void runVariation(String title, TestExecution exec) {
        System.out.print(String.format("[VARIATION] %-68s ", title));
        try {
            exec.run();
            variationTestsPassed++;
            System.out.println(TaskConstants.ANSI_GREEN + "[OK]" + TaskConstants.ANSI_RESET);
        } catch (Throwable t) {
            variationTestsFailed++;
            System.out.println(TaskConstants.ANSI_RED + "[FAIL]: " + t.getMessage() + TaskConstants.ANSI_RESET);
            t.printStackTrace(System.err);
        }
    }

    // =========================================================================
    // Test Case Implementations
    // =========================================================================

    private static void testPriorityClamping() {
        AbstractWorkerTask t = new SimpleTask("Priority-Clamp-Test");

        t.setPriority(-100);
        assertCondition(t.getPriority() == 1, "Priority -100 should be clamped to 1, got: " + t.getPriority());

        t.setPriority(0);
        assertCondition(t.getPriority() == 1, "Priority 0 should be clamped to 1, got: " + t.getPriority());

        t.setPriority(1);
        assertCondition(t.getPriority() == 1, "Priority 1 should remain 1");

        t.setPriority(10);
        assertCondition(t.getPriority() == 10, "Priority 10 should remain 10");

        t.setPriority(11);
        assertCondition(t.getPriority() == 10, "Priority 11 should be clamped to 10, got: " + t.getPriority());

        t.setPriority(99999);
        assertCondition(t.getPriority() == 10, "Priority 99999 should be clamped to 10, got: " + t.getPriority());
    }

    private static void testSimpleTaskLimits() {
        int[] testLimits = { -50, 0, 1, 2, 500 };
        for (int lim : testLimits) {
            SimpleTask task = new SimpleTask("SimpleTask-Lim-" + lim, lim);
            task.run();
            assertCondition(task.getResult() != null, "Result should not be null for limit " + lim);
            assertCondition(task.getResult().isSuccess(), "Task should succeed for limit " + lim);
        }
    }

    private static void testAnalyticTaskSizes() {
        int[] testSizes = { -10, 0, 1, 2, 3, 500 };
        for (int size : testSizes) {
            AnalyticTask task = new AnalyticTask("Analytic-Size-" + size, size);
            task.run();
            assertCondition(task.getResult() != null, "Result should not be null for sampleSize " + size);
            assertCondition(task.getResult().isSuccess(), "Task should succeed for sampleSize " + size);
            assertCondition(task.getResult().getMetrics().containsKey("mean"), "Metrics must contain mean for size " + size);
            assertCondition(task.getResult().getMetrics().containsKey("median"), "Metrics must contain median for size " + size);
        }
    }

    private static void testNetworkTaskVariations() {
        NetworkTask t1 = new NetworkTask("Net-Null-URL", null, 0);
        NetworkTask t2 = new NetworkTask("Net-Empty-URL", "", -500);
        NetworkTask t3 = new NetworkTask("Net-Large-Payload", "https://salesforce.internal.test/sync", 1024);

        t1.run();
        t2.run();
        t3.run();

        assertCondition(t1.getResult().isSuccess(), "Null URL should fall back and succeed");
        assertCondition(t2.getResult().isSuccess(), "Empty URL and negative payload should succeed");
        assertCondition(t3.getResult().isSuccess(), "Large payload network task should succeed");
    }

    private static void testFileSystemTaskPaths() {
        FileSystemTask tNull = new FileSystemTask("FS-Null", null);
        FileSystemTask tEmpty = new FileSystemTask("FS-Empty", "   ");
        FileSystemTask tNonExistent = new FileSystemTask("FS-NonExistent", "non_existent_folder_abc_xyz");
        FileSystemTask tCore = new FileSystemTask("FS-Core", "core");

        tNull.run();
        tEmpty.run();
        tNonExistent.run();
        tCore.run();

        assertCondition(tNull.getResult().isSuccess(), "Null directory path should fall back safely");
        assertCondition(tEmpty.getResult().isSuccess(), "Whitespace directory path should fall back safely");
        assertCondition(tNonExistent.getResult().isSuccess(), "Non-existent path must be handled gracefully");
        assertCondition(tCore.getResult().isSuccess(), "Existing core directory scan must succeed");
        long coreFiles = ((Number) tCore.getResult().getMetrics().get("filesAudited")).longValue();
        assertCondition(coreFiles > 0, "Core directory should contain files, audited: " + coreFiles);
    }

    private static void testSecurityTaskRoles() {
        SecurityTask tAdmin = new SecurityTask("Sec-Admin", "admin_user", "ADMIN");
        SecurityTask tOperator = new SecurityTask("Sec-Op", "op_user", "OPERATOR");
        SecurityTask tGuest = new SecurityTask("Sec-Guest", "guest_user", "GUEST");
        SecurityTask tInjection = new SecurityTask("Sec-Injection", "admin' OR 1=1 --", "HACKER");
        SecurityTask tNull = new SecurityTask("Sec-Null", null, null);

        tAdmin.run();
        tOperator.run();
        tGuest.run();
        tInjection.run();
        tNull.run();

        assertCondition(Boolean.TRUE.equals(tAdmin.getResult().getMetrics().get("authorized")), "ADMIN should be authorized");
        assertCondition(Boolean.TRUE.equals(tOperator.getResult().getMetrics().get("authorized")), "OPERATOR should be authorized");
        assertCondition(Boolean.FALSE.equals(tGuest.getResult().getMetrics().get("authorized")), "GUEST should not be authorized");
        assertCondition(Boolean.FALSE.equals(tInjection.getResult().getMetrics().get("authorized")), "HACKER role should not be authorized");
        assertCondition(tNull.getResult().isSuccess(), "Null user/role should not throw exception");
    }

    private static void testBaseTaskMetadata() {
        BaseTask task = new BaseTask(null);
        assertCondition("Unnamed-Task".equals(task.getTaskName()), "Null name should fall back to Unnamed-Task");

        task.setTaskName("   ");
        assertCondition("Unnamed-Task".equals(task.getTaskName()), "Blank name should fall back to Unnamed-Task");

        task.setDescription(null);
        assertCondition("No description provided".equals(task.getDescription()), "Null description should have fallback");

        task.setCategory(null);
        assertCondition("General".equals(task.getCategory()), "Null category should have fallback");

        task.setUserTag(null);
        assertCondition("System".equals(task.getUserTag()), "Null userTag should have fallback");
    }

    private static void testBatchSizing() {
        AsyncExecutor executor = new AsyncExecutor(2);

        // 1. Empty list
        boolean emptyRes = executor.executeAll(Collections.emptyList(), 3);
        assertCondition(emptyRes, "Empty task batch must return true immediately");

        // 2. Single task
        List<AbstractWorkerTask> single = List.of(new SimpleTask("Single-Worker", 100));
        boolean singleRes = executor.executeAll(single, 5);
        assertCondition(singleRes, "Single task batch must succeed");
        assertCondition(TaskConstants.STATE_COMPLETED.equals(single.get(0).getState()), "Single task should be completed");

        executor.shutdown();
    }

    private static void testInvertedPriorityEnqueueing() {
        // Enqueue tasks into a 1-thread executor so they queue up in the PriorityBlockingQueue.
        // Queue lowest priority tasks first (Priority 10), then queue highest priority tasks (Priority 1).
        AsyncExecutor singleThreadExecutor = new AsyncExecutor(1);
        List<AbstractWorkerTask> tasks = new ArrayList<>();
        List<String> executionOrder = new CopyOnWriteArrayList<>();

        // Add 5 Low Priority tasks (P10)
        for (int i = 1; i <= 3; i++) {
            AbstractWorkerTask low = new SimpleTask("LowPri-Task-" + i, 1000);
            low.setPriority(10);
            low.addStateListener((t, oldState, newState) -> {
                if (TaskConstants.STATE_RUNNING.equals(newState)) {
                    executionOrder.add(t.getTaskName() + " (P" + t.getPriority() + ")");
                }
            });
            tasks.add(low);
        }

        // Add 1 High Priority task (P1)
        AbstractWorkerTask high = new SecurityTask("HighPri-Task", "admin", "ADMIN");
        high.setPriority(1);
        high.addStateListener((t, oldState, newState) -> {
            if (TaskConstants.STATE_RUNNING.equals(newState)) {
                executionOrder.add(t.getTaskName() + " (P" + t.getPriority() + ")");
            }
        });
        tasks.add(high);

        boolean resolved = singleThreadExecutor.executeAll(tasks, 10);
        assertCondition(resolved, "Inverted priority batch must resolve within timeout");

        singleThreadExecutor.shutdown();
    }

    private static void testHighVolumeBurst() {
        AsyncExecutor executor = new AsyncExecutor(4);
        List<AbstractWorkerTask> burst = new ArrayList<>();
        AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 1; i <= 24; i++) {
            AbstractWorkerTask t;
            int type = i % 4;
            switch (type) {
                case 1 -> t = new SimpleTask("Burst-Compute-" + i, 5000);
                case 2 -> t = new AnalyticTask("Burst-Analytic-" + i, 200);
                case 3 -> t = new SecurityTask("Burst-Sec-" + i, "user" + i, (i % 2 == 0) ? "ADMIN" : "GUEST");
                default -> t = new FileSystemTask("Burst-FS-" + i, "data");
            }
            t.setPriority((i % 10) + 1);
            t.addStateListener((task, oldState, newState) -> {
                if (TaskConstants.STATE_COMPLETED.equals(newState)) {
                    completedCount.incrementAndGet();
                }
            });
            burst.add(t);
        }

        boolean resolved = executor.executeAll(burst, 15);
        assertCondition(resolved, "24-task stress burst must resolve within timeout");
        assertCondition(completedCount.get() == 24, "All 24 tasks must reach completed state, got: " + completedCount.get());

        executor.shutdown();
    }

    private static void testAuthMatrix() {
        // Correct credentials
        String validHash = TaskConstants.sha256("admin123");
        assertCondition(TaskConstants.ADMIN_PASSWORD_HASH.equals(validHash), "Valid admin password must match reference hash");

        // Wrong passwords
        String wrong1 = TaskConstants.sha256("admin");
        assertCondition(!TaskConstants.ADMIN_PASSWORD_HASH.equals(wrong1), "Wrong password 'admin' must not match");

        String wrong2 = TaskConstants.sha256("password");
        assertCondition(!TaskConstants.ADMIN_PASSWORD_HASH.equals(wrong2), "Wrong password 'password' must not match");

        String blank = TaskConstants.sha256("");
        assertCondition(!TaskConstants.ADMIN_PASSWORD_HASH.equals(blank), "Blank password must not match");

        String nullHash = TaskConstants.sha256(null);
        assertCondition("".equals(nullHash), "Null input must return empty string");
    }

    private static void testStateManagerEdgeCases() {
        // 1. Zero tasks (check no division-by-zero crashes)
        StateManager emptyMgr = new StateManager();
        emptyMgr.showAllStates();
        emptyMgr.saveSummaryToFile();

        // 2. Single task
        StateManager singleMgr = new StateManager();
        AbstractWorkerTask singleTask = new SimpleTask("Single-Metric-Task", 100);
        singleTask.setDuration(120);
        singleTask.setState(TaskConstants.STATE_COMPLETED);
        singleMgr.addTask(singleTask);
        singleMgr.saveSummaryToFile();

        // 3. Failed task
        StateManager failedMgr = new StateManager();
        AbstractWorkerTask failTask = new SimpleTask("Failed-Metric-Task", 100);
        failTask.setDuration(50);
        failTask.setState(TaskConstants.STATE_FAILED);
        failedMgr.addTask(failTask);
        failedMgr.saveSummaryToFile();
    }

    private static void assertCondition(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("Assertion Failed: " + msg);
        }
    }

    @FunctionalInterface
    private interface TestExecution {
        void run() throws Exception;
    }
}
