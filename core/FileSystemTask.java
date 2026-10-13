package core;

import utils.FileService;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * File system audit worker inspecting storage and calculating cryptographic checksums.
 * Demonstrates: File I/O, cryptographic hashing (SHA-256), and resource auditing.
 */
public class FileSystemTask extends AbstractWorkerTask {
    private final String targetDirectory;

    public FileSystemTask(String name) {
        this(name, "data");
    }

    public FileSystemTask(String name, String targetDirectory) {
        super(name);
        this.targetDirectory = (targetDirectory == null || targetDirectory.trim().isEmpty()) ? "data" : targetDirectory;
        this.setPriority(3); // Elevated priority for administrative I/O
    }

    @Override
    protected TaskResult doWork() throws Exception {
        log("FILESYS", "Initiating cryptographic directory audit for: " + targetDirectory);
        long start = System.currentTimeMillis();

        File dir = new File(targetDirectory);
        FileService.ensureDirectoryExists(targetDirectory + "/dummy.tmp");

        int filesAudited = 0;
        long totalBytes = 0;
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    filesAudited++;
                    totalBytes += file.length();
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            sha256.update(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }

        byte[] hashBytes = sha256.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String rootChecksum = hexString.toString();

        long elapsed = System.currentTimeMillis() - start;
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("directory", targetDirectory);
        metrics.put("filesAudited", filesAudited);
        metrics.put("totalBytes", totalBytes);
        metrics.put("aggregateSHA256", rootChecksum.substring(0, Math.min(16, rootChecksum.length())) + "...");
        metrics.put("durationMs", elapsed);

        log("FILESYS", String.format("Audit complete: %d files (%d bytes), SHA256=[%s]",
                filesAudited, totalBytes, metrics.get("aggregateSHA256")));

        return TaskResult.success("File system audit and checksum calculation complete", elapsed, metrics);
    }
}
