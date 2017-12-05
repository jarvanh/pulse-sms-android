package xyz.klinker.messenger.shared.service.jobs

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import xyz.klinker.messenger.api.entity.AddContactRequest
import xyz.klinker.messenger.api.entity.ContactBody
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.TimeUtils
import java.util.*

class ContactSyncJob : BackgroundJob() {

    override fun onRunJob(parameters: JobParameters?) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val since = sharedPrefs.getLong("last_contact_update_timestamp", -1L)

        // if they have never run this service before, then we can just write the current timestamp
        // to shared prefs and run again in 24 hours

        if (since == -1L) {
            writeUpdateTimestamp(sharedPrefs)
            scheduleNextRun(this)
            return
        }

        // otherwise, we should look for the contacts that have changed since the last run and
        // upload those contacts

        val account = Account
        if (account.encryptor == null) {
            return
        }

        val source = DataSource

        val contactsList = ContactUtils.queryNewContacts(this, source, since)
        if (contactsList.isEmpty()) {
            writeUpdateTimestamp(sharedPrefs)
            scheduleNextRun(this)
            return
        }

        source.insertContacts(this, contactsList, null)

        val contacts = arrayOfNulls<ContactBody>(contactsList.size)

        // create the array of encrypted contacts
        for (i in contactsList.indices) {
            val c = contactsList[i]
            c.encrypt(account.encryptor!!)
            val contactBody = ContactBody(c.phoneNumber, c.idMatcher, c.name, c.colors.color,
                    c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent)

            contacts[i] = contactBody
        }

        // send the contacts to our backend
        val request = AddContactRequest(Account.accountId, contacts)
        ApiUtils.addContact(request)

        // set the "since" time for our change listener
        writeUpdateTimestamp(sharedPrefs)
        scheduleNextRun(this)
    }

    private fun writeUpdateTimestamp(sharedPrefs: SharedPreferences) {
        sharedPrefs.edit().putLong("last_contact_update_timestamp", Date().time).apply()
    }

    companion object {

        private val JOB_ID = 13

        fun scheduleNextRun(context: Context) {
            val component = ComponentName(context, ContactSyncJob::class.java)
            val builder = JobInfo.Builder(JOB_ID, component)
                    .setMinimumLatency(TimeUtils.millisUntilHourInTheNextDay(2).toLong()) // 2 AM
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(true)

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(builder.build())
        }
    }
}
