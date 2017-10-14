package xyz.klinker.messenger.shared.service.jobs;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
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

public class ContactSyncJob extends BackgroundJob {

    private static final int JOB_ID = 13;

    @Override
    protected void onRunJob(JobParameters parameters) {
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

        Account account = Account.INSTANCE;
        if (account.getEncryptor() == null) {
            return;
        }

        DataSource source = DataSource.INSTANCE;

        List<Contact> contactsList = ContactUtils.queryNewContacts(this, source, since);
        if (contactsList.size() == 0) {
            writeUpdateTimestamp(sharedPrefs);
            scheduleNextRun(this);
            return;
        }

        source.insertContacts(this, contactsList, null);

        ContactBody[] contacts = new ContactBody[contactsList.size()];

        // create the array of encrypted contacts
        for (int i = 0; i < contactsList.size(); i++) {
            Contact c = contactsList.get(i);
            c.encrypt(account.getEncryptor());
            ContactBody contactBody = new ContactBody(c.getPhoneNumber(), c.getName(), c.getColors().getColor(),
                    c.getColors().getColorDark(), c.getColors().getColorLight(), c.getColors().getColorAccent());

            contacts[i] = contactBody;
        }

        // send the contacts to our backend
        AddContactRequest request =
                new AddContactRequest(Account.INSTANCE.getAccountId(), contacts);
        ApiUtils.INSTANCE.addContact(request);

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
                .setMinimumLatency(TimeUtils.INSTANCE.millisUntilHourInTheNextDay(2)) // 2 AM
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(true);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }
}
