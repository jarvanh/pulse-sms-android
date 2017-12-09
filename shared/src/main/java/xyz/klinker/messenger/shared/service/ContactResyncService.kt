package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.util.ContactUtils

class ContactResyncService : IntentService("ContactResyncService") {
    companion object {
        val TAG = "ContactResyncService"

        fun runIfApplicable(context: Context, sharedPreferences: SharedPreferences, storedAppVersion: Int) {
            if (sharedPreferences.getBoolean("v2.6.6.8", true)) {
                if (storedAppVersion != 0) {
                    context.startService(Intent(context, ContactResyncService::class.java))
                }

                sharedPreferences.edit()
                        .putBoolean("v2.6.6.8", false)
                        .commit()
            }
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        val encryptionUtils = Account.encryptor
        val startTime = System.currentTimeMillis()
        val removed = DataSource.deleteAllContacts(this)

        Log.v(TAG, "deleted all contacts: ${System.currentTimeMillis() - startTime} ms")

        if (removed > 0 && Account.exists() && Account.primary) {
            ApiUtils.clearContacts(Account.accountId)
            Log.v(TAG, "deleting all contacts on web: ${System.currentTimeMillis() - startTime} ms")
        }

        val contacts = ContactUtils.queryContacts(this, DataSource)
        DataSource.insertContacts(this, contacts, null)
        Log.v(TAG, "queried and inserted new contacts: ${System.currentTimeMillis() - startTime} ms")

        if (Account.exists() && Account.primary) {
            ApiUploadService.uploadContacts(this, encryptionUtils!!)
            Log.v(TAG, "uploaded contact changes: ${System.currentTimeMillis() - startTime} ms")
        }
    }
}
