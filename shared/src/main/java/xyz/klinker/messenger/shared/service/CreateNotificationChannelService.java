package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.util.AndroidVersionUtil;
import xyz.klinker.messenger.shared.util.NotificationUtils;

public class CreateNotificationChannelService extends IntentService {

    private static final int FOREGROUND_ID = 1224;

    public CreateNotificationChannelService() {
        super("CreateNotificationChannelService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        Notification notification = new NotificationCompat.Builder(this,
                NotificationUtils.GENERAL_CHANNEL_ID)
                .setContentTitle(getString(R.string.downloading_and_decrypting))
                .setSmallIcon(R.drawable.ic_stat_notify_group)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(getResources().getColor(R.color.colorPrimary))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
        startForeground(FOREGROUND_ID, notification);

        NotificationUtils.createNotificationChannels(this);

        stopForeground(true);
    }
}
