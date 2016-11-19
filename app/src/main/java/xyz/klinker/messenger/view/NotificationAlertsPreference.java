package xyz.klinker.messenger.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Set;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.service.RepeatNotificationService;

public class NotificationAlertsPreference extends Preference implements
        Preference.OnPreferenceClickListener {

    private static final int RINGTONE_REQUEST = 101;

    public NotificationAlertsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public NotificationAlertsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public NotificationAlertsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NotificationAlertsPreference(Context context) {
        super(context);
        init();
    }

    public void init() {
        setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        View layout = LayoutInflater.from(getContext()).inflate(R.layout.preference_notifications, null, false);

        layout.findViewById(R.id.vibrate).setOnClickListener(view -> vibrateClicked());
        layout.findViewById(R.id.ringtone).setOnClickListener(view -> ringtoneClicked());
        layout.findViewById(R.id.repeat).setOnClickListener(view -> repeatClicked());

        new AlertDialog.Builder(getContext(), R.style.SubscriptionPicker)
                .setView(layout)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .setNegativeButton(R.string.test, (dialogInterface, i) -> {
                    makeTestNotification();
                }).show();

        return false;
    }

    public boolean handleRingtoneResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RINGTONE_REQUEST) {

            if (data != null) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

                if (callChangeListener(uri != null ? uri.toString() : "")) {
                    Settings.get(getContext()).ringtone = uri.toString();
                    Settings.get(getContext()).setValue(getContext().getString(R.string.pref_ringtone), uri.toString());
                } else {
                    Settings.get(getContext()).ringtone = null;
                    Settings.get(getContext()).setValue(getContext().getString(R.string.pref_ringtone), null);
                }
            }

            return true;
        }

        return false;
    }

    private void repeatClicked() {
        final Settings settings = Settings.get(getContext());
        final SharedPreferences prefs = settings.getSharedPrefs();
        final String currentPattern = prefs.getString(getContext().getString(R.string.pref_repeat_notifications), "never");

        int actual = 0;
        for (String s : getContext().getResources().getStringArray(R.array.repeat_values)) {
            if (s.equals(currentPattern)) {
                break;
            } else {
                actual++;
            }
        }

        new AlertDialog.Builder(getContext(), R.style.SubscriptionPicker)
                .setSingleChoiceItems(R.array.repeat, actual, (dialogInterface, i) -> {
                    String newRepeat = getContext().getResources().getStringArray(R.array.repeat_values)[i];

                    settings.setValue(getContext().getString(R.string.pref_repeat_notifications), newRepeat);
                    new ApiUtils().updateRepeatNotifications(Account.get(getContext()).accountId, newRepeat);

                    dialogInterface.dismiss();
                }).show();
    }

    private void vibrateClicked() {
        final Settings settings = Settings.get(getContext());
        final SharedPreferences prefs = settings.getSharedPrefs();
        final String currentPattern = prefs.getString(getContext().getString(R.string.pref_vibrate), "vibrate_default");

        int actual = 0;
        for (String s : getContext().getResources().getStringArray(R.array.vibrate_values)) {
            if (s.equals(currentPattern)) {
                break;
            } else {
                actual++;
            }
        }

        new AlertDialog.Builder(getContext(), R.style.SubscriptionPicker)
                .setSingleChoiceItems(R.array.vibrate, actual, (dialogInterface, i) -> {
                    String newPattern = getContext().getResources().getStringArray(R.array.vibrate_values)[i];

                    settings.setValue(getContext().getString(R.string.pref_vibrate), newPattern);
                    new ApiUtils().updateVibrate(Account.get(getContext()).accountId, newPattern);

                    dialogInterface.dismiss();
                }).show();
    }

    private void ringtoneClicked() {
        Settings settings = Settings.get(getContext());

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                settings.ringtone != null && !settings.ringtone.isEmpty() ? Uri.parse(settings.ringtone) : null);

        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getTitle());

        ((Activity)getContext()).startActivityForResult(intent, RINGTONE_REQUEST);
    }

    private void makeTestNotification() {
        Settings settings = Settings.get(getContext());
        Settings.VibratePattern vibratePattern = settings.vibrate;
        int defaults = 0;
        if (vibratePattern == Settings.VibratePattern.DEFAULT) {
            defaults = Notification.DEFAULT_VIBRATE;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext())
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setContentTitle("Test Notification")
                .setContentText("Here is a test notification!")
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(settings.globalColorSet.color)
                .setDefaults(defaults)
                .setPriority(Notification.PRIORITY_HIGH)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis());

        Uri sound = getRingtone();
        if (sound != null) {
            builder.setSound(sound);
        }

        if (vibratePattern.pattern != null) {
            builder.setVibrate(vibratePattern.pattern);
        } else if (vibratePattern == Settings.VibratePattern.OFF) {
            builder.setVibrate(new long[0]);
        }

        NotificationManagerCompat.from(getContext()).notify(1, builder.build());
    }

    private Uri getRingtone() {
        String globalUri = Settings.get(getContext()).ringtone;
        if (globalUri != null && globalUri.isEmpty()) {
            return null;
        } if (globalUri == null) {
            // there is no global ringtone defined, or it doesn't exist on the system
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            // the global ringtone is available to use
            return Uri.parse(globalUri);
        }
    }
}
