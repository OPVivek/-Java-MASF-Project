package core;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Security worker performing token synthesis, cryptographic validation, and privilege auditing.
 * Demonstrates: Cryptography, secure random generation, and RBAC authentication.
 */
public class SecurityTask extends AbstractWorkerTask {
    private final String subjectUser;
    private final String requestedRole;

    public SecurityTask(String name) {
        this(name, "admin_user", "ADMIN");
    }

    public SecurityTask(String name, String subjectUser, String requestedRole) {
        super(name);
        this.subjectUser = (subjectUser == null || subjectUser.trim().isEmpty()) ? "Anonymous" : subjectUser;
        this.requestedRole = (requestedRole == null || requestedRole.trim().isEmpty()) ? "GUEST" : requestedRole;
        this.setPriority(1); // Critical priority (highest priority in system)
    }

    @Override
    protected TaskResult doWork() throws Exception {
        log("AUTH", "Auditing access authorization for user: " + subjectUser + " [Role: " + requestedRole + "]");
        long start = System.currentTimeMillis();

        // Simulate security policy evaluation
        Thread.sleep(100);

        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[24];
        random.nextBytes(tokenBytes);
        String sessionToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        String signature = TaskConstants.sha256(sessionToken + subjectUser + requestedRole);
        String shortSignature = signature.substring(0, 12) + "...";

        boolean isAuthorized = "ADMIN".equalsIgnoreCase(requestedRole) || "OPERATOR".equalsIgnoreCase(requestedRole);

        long elapsed = System.currentTimeMillis() - start;
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("user", subjectUser);
        metrics.put("role", requestedRole);
        metrics.put("authorized", isAuthorized);
        metrics.put("tokenPrefix", sessionToken.substring(0, 8) + "***");
        metrics.put("signature", shortSignature);
        metrics.put("auditDurationMs", elapsed);

        log("AUTH", String.format("Authorization evaluated. Status: %s | Token Sig: %s",
                isAuthorized ? "AUTHORIZED" : "DENIED", shortSignature));

        return TaskResult.success("Security authorization token generated and validated", elapsed, metrics);
    }
}
