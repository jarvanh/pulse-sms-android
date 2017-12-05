package xyz.klinker.messenger.shared.util

import android.util.Log

object PerformanceProfiler {

    private val TAG = "PerformanceProfiler"
    private val SHOULD_PROFILE = true

    private var lastLogTime = 0L
    private val time: Long
        get() = System.currentTimeMillis()

    fun logEvent(event: String) {
        if (!SHOULD_PROFILE) {
            return
        }

        if (time - lastLogTime > 1500) {
            Log.v(TAG, "***** First Event: $event")
        } else {
            Log.v(TAG, "$event (since last event: ${time - lastLogTime} ms)")
        }

        lastLogTime = time
    }
}