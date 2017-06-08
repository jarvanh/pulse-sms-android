package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.util.AndroidVersionUtil;
import xyz.klinker.messenger.shared.util.NotificationUtils;

public class CreateNotificationChannelService extends IntentService {


    public CreateNotificationChannelService() {
        super("CreateNotificationChannelService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!AndroidVersionUtil.isAndroidO()) {
            return;
        }

        DataSource source = DataSource.getInstance(this);
        source.open();

        NotificationUtils.createNotificationChannels(this, source);

        source.close();
    }
}
