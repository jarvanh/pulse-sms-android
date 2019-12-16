package xyz.klinker.messenger.shared.service.jobs

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.*
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.service.notification.Notifier
import xyz.klinker.messenger.shared.util.TimeUtils
import java.util.concurrent.TimeUnit

class RepeatNotificationWork : BroadcastReceiver() {

    override fun onReceive(context: Context?, p1: Intent?) {
        if (context != null) {
            Notifier(context).notify()
        }
    }

    companion object {

        fun scheduleNextRun(context: Context, timeout: Long) {
            if (Account.exists() && !Account.primary) {
                return
            }

            val intent = Intent(context, RepeatNotificationWork::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 223349, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            setAlarm(context, TimeUtils.now + timeout, pendingIntent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, RepeatNotificationWork::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 223349, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

        }

        private fun setAlarm(context: Context, time: Long, pendingIntent: PendingIntent) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)

            if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            }
        }


    }

}
