package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.firebase.jobdispatcher.*
import xyz.klinker.messenger.api.entity.AddContactRequest
import xyz.klinker.messenger.api.entity.ContactBody
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.TimeUtils
import java.util.*

class ContactSyncJob : SimpleJobService() {

    override fun onRunJob(job: JobParameters): Int {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val since = sharedPrefs.getLong("last_contact_update_timestamp", -1L)

        // if they have never run this service before, then we can just write the current timestamp
        // to shared prefs and run again in 24 hours

        if (since == -1L) {
            writeUpdateTimestamp(sharedPrefs)
            scheduleNextRun(this)
            return JobService.RESULT_SUCCESS
        }

        // otherwise, we should look for the contacts that have changed since the last run and
        // upload those contacts

        val account = Account
        if (account.encryptor == null) {
            return JobService.RESULT_FAIL_NORETRY
        }

        val source = DataSource

        val contactsList = ContactUtils.queryNewContacts(this, source, since)
        if (contactsList.isEmpty()) {
            writeUpdateTimestamp(sharedPrefs)
            scheduleNextRun(this)
            return JobService.RESULT_SUCCESS
        }

        source.insertContacts(this, contactsList, null)

        val contacts = arrayOfNulls<ContactBody>(contactsList.size)

        // create the array of encrypted contacts
        for (i in contactsList.indices) {
            val c = contactsList[i]
            c.encrypt(account.encryptor!!)

            val contactBody = if (c.type != null) {
                ContactBody(c.id, c.phoneNumber, c.idMatcher, c.name, c.type!!, c.colors.color, c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent)
            } else {
                ContactBody(c.id, c.phoneNumber, c.idMatcher, c.name, c.colors.color, c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent)
            }

            contacts[i] = contactBody
        }

        // send the contacts to our backend
        val request = AddContactRequest(Account.accountId, contacts)
        ApiUtils.addContact(request)

        // set the "since" time for our change listener
        writeUpdateTimestamp(sharedPrefs)
        scheduleNextRun(this)

        return JobService.RESULT_SUCCESS
    }

    private fun writeUpdateTimestamp(sharedPrefs: SharedPreferences) {
        sharedPrefs.edit().putLong("last_contact_update_timestamp", Date().time).apply()
    }

    companion object {

        private const val JOB_ID = "contact-sync-job"

        fun scheduleNextRun(context: Context) {
            val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(context))
            val time = (TimeUtils.millisUntilHourInTheNextDay(2).toLong() / 1000).toInt()
            val myJob = dispatcher.newJobBuilder()
                    .setService(ContactSyncJob::class.java)
                    .setTag(JOB_ID)
                    .setRecurring(false)
                    .setLifetime(Lifetime.FOREVER)
                    .setTrigger(Trigger.executionWindow(time, time + (5 * TimeUtils.MINUTE.toInt() / 1000)))
                    .setReplaceCurrent(true)
                    .build()

            if (FeatureFlags.QUERY_DAILY_CONTACT_CHANGES) {
                dispatcher.mustSchedule(myJob)
            }
        }
    }
}
