import core.*;
import utils.AsyncExecutor;
import utils.FileService;
import utils.StateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point for the MASF Enterprise Task Management Engine.
 * Supports both high-speed automated benchmarking (--demo) and interactive CLI orchestration.
 */
public class Main {

    public static void main(String[] args) {
        // Initialize file streams
        FileService.clearFile(TaskConstants.LOG_FILE);
        FileService.clearFile(TaskConstants.SUMMARY_FILE);

        System.out.println(TaskConstants.BANNER);
        System.out.println("[SYSTEM] Initializing Modular Asynchronous Task Framework (MASF)...");

        // Check if demo flag was passed via command line
        if (args.length > 0 && ("--demo".equalsIgnoreCase(args[0]) || "-d".equalsIgnoreCase(args[0]))) {
            runAutomatedDemo();
            return;
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("\nSelect Execution Mode:");
        System.out.println("  [1] Automated Enterprise Demo & Concurrency Benchmark (Instant)");
        System.out.println("  [2] Interactive Task Orchestration");
        System.out.print(">> Choice (1/2, default=1): ");

        String modeChoice = scanner.nextLine().trim();
        if ("2".equals(modeChoice)) {
            runInteractiveMode(scanner);
        } else {
            runAutomatedDemo();
        }
        scanner.close();
    }

    /**
     * Automated high-concurrency benchmark demonstrating priority dispatch,
     * thread-safety, and multi-paradigm worker tasks.
     */
    public static void runAutomatedDemo() {
        System.out.println("\n" + TaskConstants.ANSI_BOLD + ">>> LAUNCHING AUTOMATED CONCURRENCY BENCHMARK & DEMO <<<" + TaskConstants.ANSI_RESET);
        System.out.println("[CONFIG] Core Worker Threads : 3 (PriorityBlockingQueue Managed)");
        System.out.println("[CONFIG] Synchronization     : CountDownLatch Deterministic Await");

        AsyncExecutor executor = new AsyncExecutor(3);
        StateManager manager = new StateManager();

        List<AbstractWorkerTask> tasks = new ArrayList<>();

        // 1. High Priority Security Task (Priority 1)
        AbstractWorkerTask t1 = new SecurityTask("Security-Audit", "vivek_admin", "ADMIN");
        t1.setCategory("Security");
        t1.setUserTag("SecOps");
        tasks.add(t1);

        // 2. Network Sync Task (Priority 2)
        AbstractWorkerTask t2 = new NetworkTask("Salesforce-Sync", "https://api.salesforce.masf/v1/telemetry", 256);
        t2.setCategory("Integration");
        t2.setUserTag("DataPipeline");
        tasks.add(t2);

        // 3. File System Checksum Task (Priority 3)
        AbstractWorkerTask t3 = new FileSystemTask("Storage-Checksum", "data");
        t3.setCategory("Maintenance");
        t3.setUserTag("SysAdmin");
        tasks.add(t3);

        // 4. Statistical Analytics Task (Priority 4)
        AbstractWorkerTask t4 = new AnalyticTask("Telemetry-P95", 5000);
        t4.setCategory("Analytics");
        t4.setUserTag("DataScience");
        tasks.add(t4);

        // 5. Heavy Compute Task (Priority 6)
        AbstractWorkerTask t5 = new SimpleTask("Prime-Compute", 60000);
        t5.setCategory("Compute");
        t5.setUserTag("BackendWorker");
        tasks.add(t5);

        // Register with state manager
        for (AbstractWorkerTask task : tasks) {
            manager.addTask(task);
        }

        System.out.println("\n--- DISPATCHING TASKS CONCURRENTLY ---");
        long startWallTime = System.currentTimeMillis();

        // Deterministic wait using CountDownLatch (No Thread.sleep guesswork)
        boolean finishedInTime = executor.executeAll(tasks, 15);
        long totalWallTime = System.currentTimeMillis() - startWallTime;

        if (finishedInTime) {
            System.out.println(TaskConstants.ANSI_GREEN + "\n[+] All tasks resolved successfully via CountDownLatch in "
                    + totalWallTime + " ms." + TaskConstants.ANSI_RESET);
        } else {
            System.out.println(TaskConstants.ANSI_RED + "\n[!] Timed out awaiting task resolution." + TaskConstants.ANSI_RESET);
        }

        // Present analytical dashboard and export summary
        manager.showAllStates();
        manager.saveSummaryToFile();

        System.out.println("[SYSTEM] Total Managed Entities Created: " + BaseEntity.getInstanceCount());
        executor.shutdown();
    }

    /**
     * Interactive CLI session for customized manual task submission.
     */
    public static void runInteractiveMode(Scanner scanner) {
        AsyncExecutor executor = new AsyncExecutor(3);
        StateManager manager = new StateManager();

        System.out.print("\n>> ENTER SESSION USER TAG: ");
        String sessionUser = scanner.nextLine().trim();
        if (sessionUser.isEmpty()) sessionUser = "Developer";

        System.out.print(">> SELECT ROLE (1=Admin, 2=Guest): ");
        String roleChoice = scanner.nextLine().trim();
        String userRole = "GUEST";

        if ("1".equals(roleChoice)) {
            System.out.print("   Username: ");
            String inputUser = scanner.nextLine().trim();
            System.out.print("   Password (default is 'admin123'): ");
            String inputPass = scanner.nextLine().trim();

            String enteredHash = TaskConstants.sha256(inputPass);
            if (inputUser.equalsIgnoreCase(TaskConstants.ADMIN_USERNAME) 
                    && enteredHash.equals(TaskConstants.ADMIN_PASSWORD_HASH)) {
                userRole = "ADMIN";
                System.out.println(TaskConstants.ANSI_GREEN + "[AUTH] Administrator access GRANTED." + TaskConstants.ANSI_RESET);
            } else {
                System.out.println(TaskConstants.ANSI_RED + "[AUTH] ERROR: Invalid credentials. Defaulting to GUEST role." + TaskConstants.ANSI_RESET);
            }
        } else {
            System.out.println("[AUTH] Authenticated as GUEST.\n");
        }

        System.out.print(">> HOW MANY TASKS TO ORCHESTRATE? (1-10): ");
        int taskCount = 1;
        try {
            taskCount = Integer.parseInt(scanner.nextLine().trim());
            if (taskCount < 1) taskCount = 1;
        } catch (NumberFormatException e) {
            System.out.println("[!] Invalid number. Defaulting to 1 task.");
        }

        List<AbstractWorkerTask> pendingTasks = new ArrayList<>();

        for (int i = 1; i <= taskCount; i++) {
            System.out.println("\n--- CONFIGURING TASK " + i + " OF " + taskCount + " ---");

            System.out.print("   Name        : ");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) name = "Task-" + i;

            System.out.print("   Description : ");
            String description = scanner.nextLine().trim();

            System.out.print("   Category (1=Dev, 2=Maint, 3=Critical): ");
            String catChoice = scanner.nextLine().trim();
            String category = switch (catChoice) {
                case "2" -> "Maintenance";
                case "3" -> "Critical";
                default -> "Development";
            };

            String typeChoice = "";
            while (true) {
                System.out.print("   Task Type (1=Compute, 2=Network, 3=Analytic, 4=File [ADMIN], 5=Security [ADMIN]): ");
                typeChoice = scanner.nextLine().trim();

                boolean isRestricted = "4".equals(typeChoice) || "5".equals(typeChoice);
                if (isRestricted && "GUEST".equals(userRole)) {
                    System.out.println(TaskConstants.ANSI_RED + "   [!] ACCESS DENIED: Only Admin can run File or Security tasks." + TaskConstants.ANSI_RESET);
                    continue;
                }

                if (List.of("1", "2", "3", "4", "5").contains(typeChoice)) {
                    break;
                }
                System.out.println("   [!] INVALID TYPE. Please select 1-5.");
            }

            int priority = 5;
            System.out.print("   Priority (1-10, 1 is highest, default=5): ");
            try {
                String priInput = scanner.nextLine().trim();
                if (!priInput.isEmpty()) {
                    priority = Integer.parseInt(priInput);
                }
            } catch (Exception e) {
                priority = 5;
            }

            AbstractWorkerTask task = switch (typeChoice) {
                case "2" -> new NetworkTask(name);
                case "3" -> new AnalyticTask(name);
                case "4" -> new FileSystemTask(name);
                case "5" -> new SecurityTask(name, sessionUser, userRole);
                default -> new SimpleTask(name);
            };

            task.setUserTag(sessionUser);
            task.setCategory(category);
            task.setPriority(priority);
            task.setDescription(description);

            manager.addTask(task);
            pendingTasks.add(task);
            System.out.println(TaskConstants.ANSI_GREEN + "[+] Task queued successfully." + TaskConstants.ANSI_RESET);
        }

        System.out.println("\n--- EXECUTING ALL TASKS ---");
        System.out.println("Status monitoring active. Awaiting deterministic completion via CountDownLatch...\n");

        executor.executeAll(pendingTasks, 30);

        System.out.println("\n--- FINAL SYSTEM REPORT ---");
        manager.showAllStates();
        manager.saveSummaryToFile();

        System.out.println("\n[SYSTEM] Orchestration complete. Total Entities: " + BaseEntity.getInstanceCount());
        executor.shutdown();
    }
}
