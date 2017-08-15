package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.util.AndroidVersionUtil;
import xyz.klinker.messenger.shared.util.NotificationUtils;

public class FirebaseResetService extends IntentService {

    private static final int FOREGROUND_ID = 1223;

    public FirebaseResetService() {
        super("FirebaseResetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Account.get(this).primary) {
            return;
        }

        if (AndroidVersionUtil.isAndroidO()) {
            Notification notification = new NotificationCompat.Builder(this,
                    NotificationUtils.GENERAL_CHANNEL_ID)
                    .setContentTitle(getString(R.string.media_parse_text))
                    .setSmallIcon(R.drawable.ic_stat_notify_group)
                    .setProgress(0, 0, true)
                    .setLocalOnly(true)
                    .setColor(ColorSet.DEFAULT(this).color)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .build();
            startForeground(FOREGROUND_ID, notification);
        }

        // going to re-download everything I guess..
        DataSource source = DataSource.getInstance(this);
        source.open();
        source.clearTables();
        source.close();

        ApiDownloadService.start(this);
    }
}
