package xyz.klinker.messenger.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ProgressBar;

import com.klinker.android.send_message.Utils;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.api.implementation.*;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.service.ApiDownloadService;
import xyz.klinker.messenger.shared.service.ApiUploadService;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.PermissionsUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;
import xyz.klinker.messenger.shared.util.listener.ProgressUpdateListener;

public class InitialLoadWearActivity extends Activity implements ProgressUpdateListener {

    private static final int SETUP_REQUEST = 54321;

    private Handler handler;
    private ProgressBar progress;
    private boolean startUploadAfterSync = false;
    private BroadcastReceiver downloadReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(xyz.klinker.messenger.R.layout.activity_initial_load_wear);

        handler = new Handler();
        requestPermissions();

        progress = (ProgressBar) findViewById(R.id.loading_progress);
    }

    private void requestPermissions() {
        if (PermissionsUtils.checkRequestMainPermissions(this)) {
            PermissionsUtils.startMainPermissionRequest(this);
        } else {
            startLogin();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        try {
            if (PermissionsUtils.processPermissionRequest(this, requestCode, permissions, grantResults)) {
                startLogin();
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent data) {
        if (requestCode == SETUP_REQUEST) {
            Settings.get(this).forceUpdate(this);

            if (responseCode == RESULT_CANCELED) {
                Account account = Account.get(this);
                account.setDeviceId(this, null);
                account.setPrimary(this, false);

                startDatabaseSync();
            } else if (responseCode == LoginActivity.RESULT_START_DEVICE_SYNC) {
                startDatabaseSync();
                startUploadAfterSync = true;
            } else if (responseCode == LoginActivity.RESULT_START_NETWORK_SYNC) {
                ApiDownloadService.start(this);
                downloadReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        close();
                    }
                };

                registerReceiver(downloadReceiver,
                        new IntentFilter(ApiDownloadService.ACTION_DOWNLOAD_FINISHED));
            } else if (responseCode == xyz.klinker.messenger.api.implementation.ActivateActivity.RESULT_FAILED) {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (downloadReceiver != null) {
            unregisterReceiver(downloadReceiver);
            downloadReceiver = null;
        }
    }

    private void startLogin() {
        // we want to pass the extras from the last intent to this one, since they will tell us if
        // we should automatically skip the login and just go into the data load.
        Intent login = new Intent(this, ActivateWearActivity.class);
        login.putExtras(getIntent());

        startActivityForResult(login, SETUP_REQUEST);
    }

    private void startDatabaseSync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Context context = getApplicationContext();
                long startTime = System.currentTimeMillis();

                String myName = getName();
                String myPhoneNumber = PhoneNumberUtils.format(getPhoneNumber());

                final Account account = Account.get(context);
                account.setName(InitialLoadWearActivity.this, myName);
                account.setPhoneNumber(InitialLoadWearActivity.this, myPhoneNumber);

                DataSource source = DataSource.Companion.getInstance(context);
                source.open();

                List<Conversation> conversations = SmsMmsUtils.queryConversations(context);
                source.insertConversations(conversations, context, InitialLoadWearActivity.this);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        progress.setIndeterminate(true);
                    }
                });

                List<Contact> contacts = ContactUtils.queryContacts(context, source);
                source.insertContacts(contacts, null);
                source.close();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        close();
                    }
                }, 5000);

                Log.v("initial_load", "load took " +
                        (System.currentTimeMillis() - startTime) + " ms");
            }
        }).start();
    }

    private void close() {
        Settings.get(this).setValue(this, getString(R.string.pref_first_start), false);

        startActivity(new Intent(this, MessengerActivity.class));

        if (startUploadAfterSync) {
            ApiUploadService.start(this);
        }

        finish();
    }

    private String getName() {
        try {
            Cursor cursor = getContentResolver()
                    .query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                cursor.moveToFirst();
                String name = cursor.getString(cursor.getColumnIndex("display_name"));
                cursor.close();
                return name;
            } else {
                try {
                    cursor.close();
                } catch (Exception e) { }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private String getPhoneNumber() {
        try {
            return PhoneNumberUtils.clearFormatting(Utils.getMyPhoneNumber(this));
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void onProgressUpdate(final int current, final int max) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                progress.setIndeterminate(false);
                progress.setMax(max);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    progress.setProgress(current, true);
                } else {
                    progress.setProgress(current);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // don't let them back out of this
    }

}
