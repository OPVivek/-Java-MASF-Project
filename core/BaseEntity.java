package core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Base root class establishing global identity and thread-safe instance tracking.
 * Demonstrates: Encapsulation, static members, final instance fields, and constructor chaining.
 */
public class BaseEntity {
    private final long id;
    private static final AtomicLong instanceCounter = new AtomicLong(0);

    public BaseEntity() {
        this(System.nanoTime());
    }

    public BaseEntity(long id) {
        this.id = id;
        instanceCounter.incrementAndGet();
    }

    public static long getInstanceCount() {
        return instanceCounter.get();
    }

    public static void resetInstanceCount() {
        instanceCounter.set(0);
    }

    public final long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Entity#" + id;
    }
}
