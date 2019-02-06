package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Contact
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.TimeUtils

class ContactResyncService : IntentService("ContactResyncService") {
    companion object {
        private const val TAG = "ContactResyncService"

        fun runIfApplicable(context: Context, sharedPreferences: SharedPreferences, storedAppVersion: Int) {
            if (sharedPreferences.getBoolean("v4.5.2", true)) {
                if (storedAppVersion != 0) {
                    context.startService(Intent(context, ContactResyncService::class.java))
                }

                sharedPreferences.edit()
                        .putBoolean("v4.5.2", false)
                        .commit()
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        val encryptionUtils = Account.encryptor
        val startTime = TimeUtils.now

        val contacts = ContactUtils.queryContacts(this, DataSource)
        val groups = ContactUtils.queryContactGroups(this).map { it.toContact() }

        if (contacts.isEmpty()) {
            return
        }

        val removed = DataSource.deleteAllContacts(this)

        Log.v(TAG, "deleted all contacts: ${TimeUtils.now - startTime} ms")

        if (removed > 0 && Account.exists() && Account.primary) {
            ApiUtils.clearContacts(Account.accountId)
            Log.v(TAG, "deleting all contacts on web: ${TimeUtils.now - startTime} ms")
        }

        DataSource.insertContacts(this, contacts, null)
        Log.v(TAG, "queried and inserted new contacts: ${TimeUtils.now - startTime} ms")

        DataSource.insertContacts(this, groups, null)
        Log.v(TAG, "queried and inserted new groups: ${TimeUtils.now - startTime} ms")

        if (Account.exists() && Account.primary) {
            ApiUploadService.uploadContacts(this, encryptionUtils!!)
            Log.v(TAG, "uploaded contact changes: ${TimeUtils.now - startTime} ms")
        }
    }
}
