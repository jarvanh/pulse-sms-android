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
        val startTime = System.currentTimeMillis()

        DataSource.deleteAllContacts(this)
        Log.v(TAG, "deleted all contacts: ${System.currentTimeMillis() - startTime} ms")

        val contacts = ContactUtils.queryContacts(this, DataSource)
        DataSource.insertContacts(this, contacts, null)
        Log.v(TAG, "queried and inserted new contacts: ${System.currentTimeMillis() - startTime} ms")

        val account = Account.get(this)
        val encryptionUtils = account.encryptor

        if (account.exists() && account.primary) {
            ApiUploadService.uploadContacts(this, DataSource, encryptionUtils, account, ApiUtils())
            Log.v(TAG, "uploaded contact changes: ${System.currentTimeMillis() - startTime} ms")
        }
    }
}
