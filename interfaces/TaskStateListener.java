package interfaces;

import core.AbstractWorkerTask;

/**
 * Observer interface for reacting to lifecycle state changes in worker tasks.
 */
@FunctionalInterface
public interface TaskStateListener {
    void onStateChanged(AbstractWorkerTask task, String oldState, String newState);
}
