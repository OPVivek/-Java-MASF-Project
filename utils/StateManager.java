package utils;

import core.AbstractWorkerTask;
import core.TaskConstants;
import core.TaskResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe state registry and analytical performance reporting engine.
 * Demonstrates: Thread-safe collections (CopyOnWriteArrayList), aggregation analytics, and reporting.
 */
public class StateManager {
    private final List<AbstractWorkerTask> tasks = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter REPORT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void addTask(AbstractWorkerTask task) {
        if (task != null) {
            tasks.add(task);
        }
    }

    public List<AbstractWorkerTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public void clear() {
        tasks.clear();
    }

    /**
     * Prints an executive dashboard table of all task states to the console.
     */
    public void showAllStates() {
        System.out.println("\n" + TaskConstants.ANSI_BOLD + "==========================================================================================" + TaskConstants.ANSI_RESET);
        System.out.println(String.format("%-6s | %-18s | %-12s | %-4s | %-11s | %-9s | %s",
                "ID", "TASK NAME", "CATEGORY", "PRI", "STATE", "TIME", "RESULT PAYLOAD"));
        System.out.println("------------------------------------------------------------------------------------------");

        for (AbstractWorkerTask task : tasks) {
            String color = switch (task.getState()) {
                case TaskConstants.STATE_COMPLETED -> TaskConstants.ANSI_GREEN;
                case TaskConstants.STATE_FAILED -> TaskConstants.ANSI_RED;
                case TaskConstants.STATE_RUNNING -> TaskConstants.ANSI_YELLOW;
                default -> TaskConstants.ANSI_CYAN;
            };

            String payloadSummary = "-";
            if (task.getResult() != null) {
                payloadSummary = task.getResult().getMessage();
            }

            System.out.println(String.format("%-6d | %-18s | %-12s | P%-3d | %s%-11s%s | %6d ms | %s",
                    task.getId() % 100000,
                    truncate(task.getTaskName(), 18),
                    truncate(task.getCategory(), 12),
                    task.getPriority(),
                    color, task.getState(), TaskConstants.ANSI_RESET,
                    task.getDuration(),
                    truncate(payloadSummary, 28)
            ));
        }
        System.out.println("==========================================================================================\n");
    }

    /**
     * Synthesizes performance analytics and exports a structured audit report to file.
     */
    public void saveSummaryToFile() {
        StringBuilder sb = new StringBuilder();
        String timestamp = LocalDateTime.now().format(REPORT_TIME);

        sb.append("================================================================================\n");
        sb.append("              MASF ENTERPRISE ORCHESTRATION EXECUTION REPORT                   \n");
        sb.append("================================================================================\n");
        sb.append("Generated at: ").append(timestamp).append("\n");
        sb.append("Total Registered Tasks: ").append(tasks.size()).append("\n\n");

        sb.append(String.format("%-8s %-20s %-12s %-5s %-12s %-10s %s\n",
                "TASK_ID", "NAME", "CATEGORY", "PRI", "STATUS", "DURATION", "PAYLOAD"));
        sb.append("--------------------------------------------------------------------------------\n");

        long totalDuration = 0;
        int completedCount = 0;
        int failedCount = 0;
        AbstractWorkerTask slowest = null;
        AbstractWorkerTask fastest = null;
        Map<String, Integer> categoryDistribution = new LinkedHashMap<>();

        for (AbstractWorkerTask task : tasks) {
            String state = task.getState();
            long dur = task.getDuration();
            totalDuration += dur;

            if (TaskConstants.STATE_COMPLETED.equals(state)) completedCount++;
            if (TaskConstants.STATE_FAILED.equals(state)) failedCount++;

            categoryDistribution.put(task.getCategory(), categoryDistribution.getOrDefault(task.getCategory(), 0) + 1);

            if (slowest == null || dur > slowest.getDuration()) slowest = task;
            if (fastest == null || (dur < fastest.getDuration() && dur > 0)) fastest = task;

            String resultStr = (task.getResult() != null) ? task.getResult().getMessage() : "N/A";
            sb.append(String.format("%-8d %-20s %-12s P%-4d %-12s %6d ms   %s\n",
                    task.getId() % 100000,
                    truncate(task.getTaskName(), 20),
                    truncate(task.getCategory(), 12),
                    task.getPriority(),
                    state,
                    dur,
                    resultStr
            ));
        }

        double avgDuration = tasks.isEmpty() ? 0 : (double) totalDuration / tasks.size();

        sb.append("--------------------------------------------------------------------------------\n\n");
        sb.append("--- PERFORMANCE METRICS & SYSTEM ANALYTICS ---\n");
        sb.append(String.format("Tasks Completed Successfully : %d / %d (%.1f%%)\n",
                completedCount, tasks.size(), tasks.isEmpty() ? 0 : (completedCount * 100.0 / tasks.size())));
        sb.append(String.format("Tasks Failed / Aborted       : %d\n", failedCount));
        sb.append(String.format("Cumulative Compute Time      : %d ms\n", totalDuration));
        sb.append(String.format("Average Task Execution Time  : %.2f ms\n", avgDuration));

        if (slowest != null) {
            sb.append(String.format("System Bottleneck (Slowest)  : %s (%d ms) [Category: %s]\n",
                    slowest.getTaskName(), slowest.getDuration(), slowest.getCategory()));
        }
        if (fastest != null) {
            sb.append(String.format("Peak Performer (Fastest)     : %s (%d ms)\n",
                    fastest.getTaskName(), fastest.getDuration()));
        }

        sb.append("\nCategory Distribution:\n");
        categoryDistribution.forEach((cat, count) ->
                sb.append(String.format("  - %-15s : %d tasks\n", cat, count)));

        sb.append("================================================================================\n");

        FileService.appendToFile(TaskConstants.SUMMARY_FILE, sb.toString());
        System.out.println(TaskConstants.ANSI_GREEN + "[SYSTEM] Performance analytics exported successfully to: "
                + TaskConstants.SUMMARY_FILE + TaskConstants.ANSI_RESET);
    }

    private String truncate(String val, int maxLen) {
        if (val == null) return "-";
        if (val.length() <= maxLen) return val;
        return val.substring(0, maxLen - 3) + "...";
    }
}
