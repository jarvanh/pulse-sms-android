package xyz.klinker.messenger.shared.service.jobs;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
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
import xyz.klinker.messenger.shared.util.TimeUtils;

public class ContactSyncJob extends IntentService {

    private static final int JOB_ID = 13;

    public ContactSyncJob() {
        super("ContactSyncJob");
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
        ComponentName component = new ComponentName(context, ContactSyncJob.class);
        JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, component)
                .setMinimumLatency(TimeUtils.millisUntilHourInTheNextDay(2)) // 2 AM
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(true);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }
}
