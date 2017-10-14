package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import xyz.klinker.messenger.shared.util.TimeUtils

/**
 * Android can shut down IntentService's during transition time, if there is no wakelocks.
 * The MessengerFirebaseMessagingService (I assume) is started by a WakefulBroadcastReciever,
 * but when we transition to the FirebaseHandlerService (which wasn't started wakefully),
 * it could fail.
 *
 * This class should provide a wakeful interface for our firebase receiver to do its work in.
 */
abstract class WakefulIntentService(name: String) : IntentService(name) {

    protected abstract fun doWakefulWork(intent: Intent?)

    init {
        setIntentRedelivery(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val lock = getLock(this.applicationContext)

        if (!lock!!.isHeld || flags and Service.START_FLAG_REDELIVERY != 0) {
            lock.acquire(TimeUtils.SECOND * 15)
        }

        super.onStartCommand(intent, flags, startId)
        return Service.START_REDELIVER_INTENT
    }

    override fun onHandleIntent(intent: Intent?) {
        try {
            doWakefulWork(intent)
        } finally {
            val lock = getLock(this.applicationContext)

            if (lock!!.isHeld) {
                try {
                    lock.release()
                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, "Exception when releasing wakelock", e)
                }
            }
        }
    }

    companion object {

        private val NAME = "xyz.klinker.messenger.WakefulIntentService"

        @Volatile private var lockStatic: PowerManager.WakeLock? = null
        @Synchronized private fun getLock(context: Context): PowerManager.WakeLock? {
            if (lockStatic == null) {
                val mgr = context.getSystemService(Context.POWER_SERVICE) as PowerManager

                lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NAME)
                lockStatic!!.setReferenceCounted(true)
            }

            return lockStatic
        }
    }
}