package xyz.klinker.messenger.shared.service;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.api.entity.AddContactRequest;
import xyz.klinker.messenger.api.entity.ContactBody;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.util.ContactUtils;

public class ContactSyncService extends IntentService {

    private static final int REQUEST_CODE = 13;
    private static final long RUN_EVERY = 1000 * 60 * 60 * 24; // 1 day

    public ContactSyncService() {
        super("ContactSyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long since = sharedPrefs.getLong("last_contact_update_timestamp", -1L);

        // if they have never run this service before, then we can just write the current timestamp
        // to shared prefs and run again in 24 hours

        if (since == -1L) {
            writeUpdateTimestamp(sharedPrefs);
            scheduleNextRun(this);
            return;
        }

        // otherwise, we should look for the contacts that have changed since the last run and
        // upload those contacts

        Account account = Account.get(this);
        if (account.getEncryptor() == null) {
            return;
        }

        DataSource source = DataSource.getInstance(this);
        source.open();

        List<Contact> contactsList = ContactUtils.queryNewContacts(this, source, since);
        if (contactsList.size() == 0) {
            source.close();
            writeUpdateTimestamp(sharedPrefs);
            scheduleNextRun(this);
            return;
        }

        source.insertContacts(contactsList, null);
        source.close();

        ContactBody[] contacts = new ContactBody[contactsList.size()];

        // create the array of encrypted contacts
        for (int i = 0; i < contactsList.size(); i++) {
            Contact c = contactsList.get(i);
            c.encrypt(account.getEncryptor());
            ContactBody contactBody = new ContactBody(c.phoneNumber, c.name, c.colors.color,
                    c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent);

            contacts[i] = contactBody;
        }

        // send the contacts to our backend
        AddContactRequest request =
                new AddContactRequest(Account.get(this).accountId, contacts);
        new ApiUtils().getApi().contact().add(request);

        // set the "since" time for our change listener
        writeUpdateTimestamp(sharedPrefs);
        scheduleNextRun(this);
    }

    private void writeUpdateTimestamp(SharedPreferences sharedPrefs) {
        sharedPrefs.edit().putLong("last_contact_update_timestamp", new Date().getTime()).apply();
    }

    public static void scheduleNextRun(Context context) {
        Intent intent = new Intent(context, ContactSyncService.class);
        PendingIntent pIntent = PendingIntent.getService(context, REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        long currentTime = new Date().getTime();

        alarmManager.cancel(pIntent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, currentTime + RUN_EVERY, pIntent);
    }
}
