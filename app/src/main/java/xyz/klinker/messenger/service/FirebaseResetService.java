package xyz.klinker.messenger.service;

import android.app.IntentService;
import android.content.Intent;

import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.FeatureFlags;

public class FirebaseResetService extends IntentService {

    public FirebaseResetService() {
        super("FirebaseResetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // going to re-download everything I guess..

        DataSource source = DataSource.getInstance(this);
        source.open();
        source.clearTables();
        source.close();

        Intent download = new Intent(this, ApiDownloadService.class);
        download.putExtra(ApiDownloadService.ARG_SHOW_NOTIFICATION, false);
        startService(download);
    }
}
