package xyz.klinker.messenger.shared.service.jobs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import xyz.klinker.messenger.api.entity.AddContactRequest
import xyz.klinker.messenger.api.entity.ContactBody
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.TimeUtils
import java.util.*
import java.util.concurrent.TimeUnit

class ContactSyncWork(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val since = sharedPrefs.getLong("last_contact_update_timestamp", -1L)

        // if they have never run this service before, then we can just write the current timestamp
        // to shared prefs and run again in 24 hours

        if (since == -1L) {
            writeUpdateTimestamp(sharedPrefs)
            scheduleNextRun(context)
            return Result.success()
        }

        // otherwise, we should look for the contacts that have changed since the last run and
        // upload those contacts

        val account = Account
        if (account.encryptor == null) {
            return Result.success()
        }

        val source = DataSource

        val contactsList = ContactUtils.queryNewContacts(context, source, since)
        if (contactsList.isEmpty()) {
            writeUpdateTimestamp(sharedPrefs)
            scheduleNextRun(context)
            return Result.success()
        }

        source.insertContacts(context, contactsList, null)

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
        scheduleNextRun(context)

        return Result.success()
    }

    private fun writeUpdateTimestamp(sharedPrefs: SharedPreferences) {
        sharedPrefs.edit().putLong("last_contact_update_timestamp", Date().time).apply()
    }

    companion object {

        fun scheduleNextRun(context: Context) {
            val time = TimeUtils.millisUntilHourInTheNextDay(2)
            val work = OneTimeWorkRequest.Builder(ContactSyncWork::class.java)
                    .setInitialDelay(time, TimeUnit.MILLISECONDS)
                    .build()
            if (FeatureFlags.QUERY_DAILY_CONTACT_CHANGES) {
                try {
                    WorkManager.getInstance().enqueue(work)
                } catch (e: Exception) {
                    // can't schedule more than 100 unique tasks?
                }
            }
        }
    }
}
