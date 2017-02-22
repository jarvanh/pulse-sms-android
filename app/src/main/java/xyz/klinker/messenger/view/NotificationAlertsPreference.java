package xyz.klinker.messenger.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.Preference;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.pojo.VibratePattern;

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
        layout.findViewById(R.id.wake_screen).setOnClickListener(view -> wakeClicked());

        if (!FeatureFlags.get(getContext()).HEADS_UP) {
            layout.findViewById(R.id.heads_up).setVisibility(View.GONE);
        } else {
            layout.findViewById(R.id.heads_up).setOnClickListener(view -> headsUpClicked());
        }

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

                if (uri != null && callChangeListener(uri.toString())) {
                    Settings.get(getContext()).ringtone = uri.toString();
                    Settings.get(getContext()).setValue(getContext().getString(R.string.pref_ringtone), uri.toString());
                } else {
                    Settings.get(getContext()).ringtone = "";
                    Settings.get(getContext()).setValue(getContext().getString(R.string.pref_ringtone), "");
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

    private void wakeClicked() {
        final Settings settings = Settings.get(getContext());
        final SharedPreferences prefs = settings.getSharedPrefs();
        final String current = prefs.getString(getContext().getString(R.string.pref_wake_screen), "off");

        int actual = 0;
        for (String s : getContext().getResources().getStringArray(R.array.wake_screen_values)) {
            if (s.equals(current)) {
                break;
            } else {
                actual++;
            }
        }

        new AlertDialog.Builder(getContext(), R.style.SubscriptionPicker)
                .setSingleChoiceItems(R.array.wake_screen, actual, (dialogInterface, i) -> {
                    String newVal = getContext().getResources().getStringArray(R.array.wake_screen_values)[i];

                    settings.setValue(getContext().getString(R.string.pref_wake_screen), newVal);
                    new ApiUtils().updateWakeScreen(Account.get(getContext()).accountId, newVal);

                    dialogInterface.dismiss();
                }).show();
    }

    private void headsUpClicked() {
        final Settings settings = Settings.get(getContext());
        final SharedPreferences prefs = settings.getSharedPrefs();
        final String current = prefs.getString(getContext().getString(R.string.pref_heads_up), "on");

        int actual = 0;
        for (String s : getContext().getResources().getStringArray(R.array.wake_screen_values)) {
            if (s.equals(current)) {
                break;
            } else {
                actual++;
            }
        }

        new AlertDialog.Builder(getContext(), R.style.SubscriptionPicker)
                .setSingleChoiceItems(R.array.wake_screen, actual, (dialogInterface, i) -> {
                    String newVal = getContext().getResources().getStringArray(R.array.wake_screen_values)[i];

                    settings.setValue(getContext().getString(R.string.pref_heads_up), newVal);
                    new ApiUtils().updateHeadsUp(Account.get(getContext()).accountId, newVal);

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
        VibratePattern vibratePattern = settings.vibrate;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext())
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setContentTitle("Test Notification")
                .setContentText("Here is a test notification!")
                .setCategory(Notification.CATEGORY_MESSAGE)
                .setColor(settings.globalColorSet.color)
                .setPriority(settings.headsUp ? Notification.PRIORITY_MAX : Notification.PRIORITY_DEFAULT)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis());

        Uri sound = getRingtone();
        if (sound != null) {
            builder.setSound(sound);
        }

        Notification notification = builder.build();
        if (vibratePattern == VibratePattern.DEFAULT) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        } else if (vibratePattern == VibratePattern.OFF) {
            builder.setVibrate(new long[0]);
            notification = builder.build();
        } else if (vibratePattern.pattern != null) {
            builder.setVibrate(vibratePattern.pattern);
            notification = builder.build();
        }

        NotificationManagerCompat.from(getContext()).notify(1, notification);
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
