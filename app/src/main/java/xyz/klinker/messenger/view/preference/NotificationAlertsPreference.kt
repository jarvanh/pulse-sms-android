package xyz.klinker.messenger.view.preference

import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.FileUriExposedException
import android.preference.Preference
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi

import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.VibratePattern
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.NotificationUtils
import xyz.klinker.messenger.shared.util.TimeUtils

@Suppress("DEPRECATION")
class NotificationAlertsPreference : Preference, Preference.OnPreferenceClickListener {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context) : super(context)

    init {
        onPreferenceClickListener = this
    }

    private val ringtone: Uri?
        get() {
            val globalUri = Settings.ringtone
            if (globalUri != null && globalUri.isEmpty()) {
                return null
            }

            return if (globalUri == null) {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                Uri.parse(globalUri)
            }
        }



    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPreferenceClick(preference: Preference): Boolean {
        val layout = LayoutInflater.from(context).inflate(R.layout.preference_notifications, null, false)

        layout.findViewById<View>(R.id.vibrate).setOnClickListener { vibrateClicked() }
        layout.findViewById<View>(R.id.ringtone).setOnClickListener { ringtoneClicked() }
        layout.findViewById<View>(R.id.repeat).setOnClickListener { repeatClicked() }
        layout.findViewById<View>(R.id.wake_screen).setOnClickListener { wakeClicked() }
        layout.findViewById<View>(R.id.heads_up).setOnClickListener { headsUpClicked() }

        val builder = AlertDialog.Builder(context, R.style.SubscriptionPicker)
                .setView(layout)
                .setPositiveButton(R.string.ok) { _, _ ->  }

//        if (AndroidVersionUtil.isAndroidO) {
//            layout.findViewById<View>(R.id.vibrate).visibility = View.GONE
//            layout.findViewById<View>(R.id.ringtone).visibility = View.GONE
//            layout.findViewById<View>(R.id.heads_up).visibility = View.GONE
//
//            builder.setNeutralButton(R.string.default_channel) { _, _ -> openNotificationChannel() }
//        } else {
        builder.setNegativeButton(R.string.test) { _, _ -> makeTestNotification() }
        if (!AndroidVersionUtil.isAndroidO) {
            layout.findViewById<View>(R.id.channels_disclaimer).visibility = View.GONE
        }
//        }

        builder.show()

        return false
    }

    private fun openNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            intent.putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, NotificationUtils.DEFAULT_CONVERSATION_CHANNEL_ID)
            context.startActivity(intent)
        }
    }

    fun handleRingtoneResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == RINGTONE_REQUEST) {

            if (data != null) {
                val uri = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

                if (uri != null && callChangeListener(uri.toString())) {
                    Settings.ringtone = uri.toString()
                    Settings.setValue(context, context.getString(R.string.pref_ringtone), uri.toString())
                } else {
                    Settings.ringtone = ""
                    Settings.setValue(context, context.getString(R.string.pref_ringtone), "")
                }
            }

            return true
        }

        return false
    }

    private fun repeatClicked() {
        val prefs = Settings.getSharedPrefs(context)
        val currentPattern = prefs.getString(context.getString(R.string.pref_repeat_notifications), "never")

        val actual = context.resources.getStringArray(R.array.repeat_values)
                .takeWhile { it != currentPattern }
                .count()

        AlertDialog.Builder(context, R.style.SubscriptionPicker)
                .setSingleChoiceItems(R.array.repeat, actual) { dialogInterface, i ->
                    val newRepeat = context.resources.getStringArray(R.array.repeat_values)[i]

                    Settings.setValue(context, context.getString(R.string.pref_repeat_notifications), newRepeat)
                    ApiUtils.updateRepeatNotifications(Account.accountId, newRepeat)

                    dialogInterface.dismiss()
                }.show()
    }

    private fun wakeClicked() {
        val prefs = Settings.getSharedPrefs(context)
        val current = prefs.getString(context.getString(R.string.pref_wake_screen), "off")

        val actual = context.resources.getStringArray(R.array.wake_screen_values)
                .takeWhile { it != current }
                .count()

        AlertDialog.Builder(context, R.style.SubscriptionPicker)
                .setSingleChoiceItems(R.array.wake_screen, actual) { dialogInterface, i ->
                    val newVal = context.resources.getStringArray(R.array.wake_screen_values)[i]

                    Settings.setValue(context, context.getString(R.string.pref_wake_screen), newVal)
                    ApiUtils.updateWakeScreen(Account.accountId, newVal)

                    dialogInterface.dismiss()
                }.show()
    }

    private fun headsUpClicked() {
        if (AndroidVersionUtil.isAndroidO) {
            openNotificationChannel()
            return
        }

        val prefs = Settings.getSharedPrefs(context)
        val current = prefs.getString(context.getString(R.string.pref_heads_up), "on")

        val actual = context.resources.getStringArray(R.array.wake_screen_values)
                .takeWhile { it != current }
                .count()

        AlertDialog.Builder(context, R.style.SubscriptionPicker)
                .setSingleChoiceItems(R.array.wake_screen, actual) { dialogInterface, i ->
                    val newVal = context.resources.getStringArray(R.array.wake_screen_values)[i]

                    Settings.setValue(context, context.getString(R.string.pref_heads_up), newVal)
                    ApiUtils.updateHeadsUp(Account.accountId, newVal)

                    dialogInterface.dismiss()
                }.show()
    }

    private fun vibrateClicked() {
        if (AndroidVersionUtil.isAndroidO) {
            openNotificationChannel()
            return
        }

        val prefs = Settings.getSharedPrefs(context)
        val currentPattern = prefs.getString(context.getString(R.string.pref_vibrate), "vibrate_default")

        val actual = context.resources.getStringArray(R.array.vibrate_values)
                .takeWhile { it != currentPattern }
                .count()

        AlertDialog.Builder(context, R.style.SubscriptionPicker)
                .setSingleChoiceItems(R.array.vibrate, actual) { dialogInterface, i ->
                    val newPattern = context.resources.getStringArray(R.array.vibrate_values)[i]

                    Settings.setValue(context, context.getString(R.string.pref_vibrate), newPattern)
                    ApiUtils.updateVibrate(Account.accountId, newPattern)

                    dialogInterface.dismiss()
                }.show()
    }

    private fun ringtoneClicked() {
        if (AndroidVersionUtil.isAndroidO) {
            openNotificationChannel()
            return
        }

        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        val ringtone = Settings.ringtone
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                if (ringtone != null && !ringtone.isEmpty()) Uri.parse(ringtone) else null)

        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)

        try {
            (context as Activity).startActivityForResult(intent, RINGTONE_REQUEST)
        } catch (e: Exception) {
            Toast.makeText(context, "Your phone has refused to allow for a custom ringtone...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeTestNotification() {
        val vibratePattern = Settings.vibrate

        val builder = NotificationCompat.Builder(context, NotificationUtils.DEFAULT_CONVERSATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setContentTitle("Test Notification")
                .setContentText("Here is a test notification!")
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(Settings.mainColorSet.color)
                .setPriority(if (Settings.headsUp) Notification.PRIORITY_MAX else Notification.PRIORITY_DEFAULT)
                .setShowWhen(true)
                .setWhen(TimeUtils.now)

        val sound = ringtone
        if (sound != null) {
            builder.setSound(sound)
        }

        var notification = builder.build()
        when {
            vibratePattern === VibratePattern.DEFAULT -> notification.defaults = notification.defaults or Notification.DEFAULT_VIBRATE
            vibratePattern === VibratePattern.OFF -> {
                builder.setVibrate(LongArray(0))
                notification = builder.build()
            }
            vibratePattern.pattern != null -> {
                builder.setVibrate(vibratePattern.pattern)
                notification = builder.build()
            }
        }

        try {
            NotificationManagerCompat.from(context).notify(1, notification)
        } catch (e: FileUriExposedException) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            NotificationManagerCompat.from(context).notify(1, builder.build())
        }

    }

    companion object {
        private val RINGTONE_REQUEST = 101
    }
}
