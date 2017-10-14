package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.content.Intent
import android.util.Log
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.util.ContactUtils

class ContactResyncService : IntentService("ContactResyncService") {
    companion object {
        val TAG = "ContactResyncService"
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
