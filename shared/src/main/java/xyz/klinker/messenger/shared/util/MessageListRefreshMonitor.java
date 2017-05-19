package xyz.klinker.messenger.shared.util;

/**
 * With an app like Pulse, there is the possibility that many messages could come in quickly.
 * If that happens, then we don't want to have to refresh the list/UI every time. This class monitors
 * the number of active refreshes and tells the app whether we should refresh the UI or not.
 */
public class MessageListRefreshMonitor {
    private int runningRefreshThreads = 0;

    public void incrementRefreshThreadsCount() {
        runningRefreshThreads++;
    }

    public void decrementRefreshThreadsCount() {
        runningRefreshThreads--;
    }

    public void resetRunningThreadCount() {
        runningRefreshThreads = 0;
    }

    public boolean shouldLoadMessagesToTheUi() {
        return runningRefreshThreads == 0;
    }
}
