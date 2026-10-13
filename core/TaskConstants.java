package core;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public final class TaskConstants {

    // Task Lifecycle States
    public static final String STATE_CREATED   = "CREATED";
    public static final String STATE_QUEUED    = "QUEUED";
    public static final String STATE_RUNNING   = "RUNNING";
    public static final String STATE_COMPLETED = "COMPLETED";
    public static final String STATE_FAILED    = "FAILED";

    // File Persistence Paths
    public static final String LOG_FILE     = "data/logs/task_activity.log";
    public static final String SUMMARY_FILE = "data/reports/execution_summary.txt";

    // Authentication (Using SHA-256 hash for secure authentication, password: "admin")
    public static final String ADMIN_USERNAME = "admin";
    // SHA-256 hash of "admin123"
    public static final String ADMIN_PASSWORD_HASH = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";

    // ANSI Colors for Executive Terminal Output
    public static final String ANSI_RESET  = "\u001B[0m";
    public static final String ANSI_BLACK  = "\u001B[30m";
    public static final String ANSI_RED    = "\u001B[31m";
    public static final String ANSI_GREEN  = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE   = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN   = "\u001B[36m";
    public static final String ANSI_WHITE  = "\u001B[37m";
    public static final String ANSI_BOLD   = "\u001B[1m";

    public static final String BANNER = 
            ANSI_CYAN + "   __  _____   _______ _____\n" +
            "  /  |/  /   | / ___// ____/\n" +
            " / /|_/ / /| | \\__ \\/ /_    \n" +
            "/ /  / / ___ |___/ / __/    \n" +
            "/_/  /_/_/  |_/____/_/       \n" +
            ANSI_BOLD + " Modular Asynchronous Task Framework (v2.0-Enterprise)\n" + ANSI_RESET;

    private TaskConstants() {
        // Enforce non-instantiability
    }

    /**
     * Utility method to hash string inputs with SHA-256 for secure credential checks.
     */
    public static String sha256(String input) {
        if (input == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
