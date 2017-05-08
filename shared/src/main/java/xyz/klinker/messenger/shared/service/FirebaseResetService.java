package xyz.klinker.messenger.shared.service;

import android.app.IntentService;
import android.content.Intent;

import xyz.klinker.messenger.shared.data.DataSource;

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
        download.putExtra(ApiDownloadService.ARG_SHOW_NOTIFICATION, true);
        startService(download);
    }
}
