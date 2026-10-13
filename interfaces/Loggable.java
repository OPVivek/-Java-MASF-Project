package interfaces;

/**
 * Interface contract for logging operations across worker tasks.
 * Demonstrates: Method overloading and default interface methods.
 */
public interface Loggable {
    void log(String message);
    void log(String level, String message);
}
