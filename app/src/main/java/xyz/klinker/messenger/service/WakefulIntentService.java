package xyz.klinker.messenger.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

/**
 * Android can shut down IntentService's during transition time, if there is no wakelocks.
 * The MessengerFirebaseMessagingService (I assume) is started by a WakefulBroadcastReciever,
 * but when we transition to the FirebaseHandlerService (which wasn't started wakefully),
 * it could fail.
 *
 * This class should provide a wakeful interface for our firebase receiver to do its work in.
 */
abstract public class WakefulIntentService extends IntentService {

    abstract protected void doWakefulWork(Intent intent);

    static final String NAME = "xyz.klinker.messenger.WakefulIntentService";
    private static volatile PowerManager.WakeLock lockStatic = null;

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr =
                    (PowerManager)context.getSystemService(Context.POWER_SERVICE);

            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NAME);
            lockStatic.setReferenceCounted(true);
        }

        return(lockStatic);
    }

    public WakefulIntentService(String name) {
        super(name);
        setIntentRedelivery(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PowerManager.WakeLock lock = getLock(this.getApplicationContext());

        if (!lock.isHeld() || (flags & START_FLAG_REDELIVERY) != 0) {
            lock.acquire();
        }

        super.onStartCommand(intent, flags, startId);

        return(START_REDELIVER_INTENT);
    }

    @Override
    final protected void onHandleIntent(Intent intent) {
        try {
            doWakefulWork(intent);
        } finally {
            PowerManager.WakeLock lock = getLock(this.getApplicationContext());

            if (lock.isHeld()) {
                try {
                    lock.release();
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "Exception when releasing wakelock", e);
                }
            }
        }
    }
}