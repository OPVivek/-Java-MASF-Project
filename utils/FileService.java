package utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Thread-safe file persistence utility for system telemetry and reports.
 * Demonstrates: NIO.2 file handling, synchronization, and robust exception safety.
 */
public class FileService {

    private FileService() {
        // Utility class
    }

    public static synchronized void appendToFile(String fileName, String content) {
        try {
            ensureDirectoryExists(fileName);
            Path path = Paths.get(fileName);
            byte[] bytes = (content + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
            Files.write(path, bytes, 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.WRITE, 
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[FILE ERROR] Failed writing to " + fileName + ": " + e.getMessage());
        }
    }

    public static synchronized void clearFile(String fileName) {
        try {
            ensureDirectoryExists(fileName);
            Path path = Paths.get(fileName);
            Files.write(path, new byte[0], 
                    StandardOpenOption.CREATE, 
                    StandardOpenOption.WRITE, 
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[FILE ERROR] Failed clearing " + fileName + ": " + e.getMessage());
        }
    }

    public static void ensureDirectoryExists(String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
