package xyz.klinker.messenger.shared.service.jobs

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.firebase.jobdispatcher.*
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.service.notification.Notifier
import xyz.klinker.messenger.shared.util.TimeUtils
import java.util.*

class RepeatNotificationJob : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            return
        }

        Thread {
            Notifier(context).notify()
        }.start()
    }

    companion object {

        fun scheduleNextRun(context: Context, nextRun: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, RepeatNotificationJob::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            if (nextRun == 0L) {
                alarmManager.cancel(pendingIntent)
                return
            }

            if (!Account.exists() || (Account.exists() && Account.primary)) {
                try {
                    if (TimeUtils.now < nextRun) {
                        setAlarm(context, nextRun, pendingIntent)
                    } else {
                        alarmManager.cancel(pendingIntent)
                    }
                } catch (e: Throwable) {
                    // can't schedule for less than 0
                }
            }
        }

        private fun setAlarm(context: Context, time: Long, pendingIntent: PendingIntent) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            }
        }

    }
}
