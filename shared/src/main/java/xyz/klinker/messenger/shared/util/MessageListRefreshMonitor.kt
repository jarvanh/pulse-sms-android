package xyz.klinker.messenger.shared.util

/**
 * With an app like Pulse, there is the possibility that many messages could come in quickly.
 * If that happens, then we don't want to have to refresh the list/UI every time. This class monitors
 * the number of active refreshes and tells the app whether we should refresh the UI or not.
 */
class MessageListRefreshMonitor {
    private var runningRefreshThreads = 0

    fun incrementRefreshThreadsCount() {
        runningRefreshThreads++
    }

    fun decrementRefreshThreadsCount() {
        runningRefreshThreads--
    }

    fun resetRunningThreadCount() {
        runningRefreshThreads = 0
    }

    fun shouldLoadMessagesToTheUi(): Boolean {
        return runningRefreshThreads == 0
    }
}
