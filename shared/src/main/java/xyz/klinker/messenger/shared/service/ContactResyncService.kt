package xyz.klinker.messenger.shared.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import xyz.klinker.messenger.api.entity.ContactBody
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.TimeUtils
import java.lang.Exception

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
        if (!Account.exists() || !Account.primary) {
            return
        }

        val encryptionUtils = Account.encryptor
        val startTime = TimeUtils.now

        val contacts = ContactUtils.queryContacts(this, DataSource).toMutableList()
        Log.v(TAG, "queried ${contacts.size} contacts: ${TimeUtils.now - startTime} ms")

        contacts.addAll(ContactUtils.queryContactGroups(this).map { it.toContact() })
        Log.v(TAG, "queried ${contacts.size} contacts + groups: ${TimeUtils.now - startTime} ms")

        if (contacts.isEmpty()) {
            return
        }

        DataSource.deleteAllContacts(this)
        Log.v(TAG, "deleted old contacts: ${TimeUtils.now - startTime} ms")

        ApiUtils.clearContacts(Account.accountId)
        Log.v(TAG, "deleting all contacts on web: ${TimeUtils.now - startTime} ms")

        Thread {
            try {
                Thread.sleep(TimeUtils.SECOND * 10)
            } catch (e: Exception) { }

            DataSource.insertContacts(this, contacts, null)
            Log.v(TAG, "inserted contacts and groups: ${TimeUtils.now - startTime} ms")

            val contactBodies = mutableListOf<ContactBody>()
            contacts.forEach {
                val c = it

                try {
                    c.encrypt(encryptionUtils!!)
                    contactBodies.add(if (c.type != null) {
                        ContactBody(c.id, c.phoneNumber, c.idMatcher, c.name, c.type!!, c.colors.color, c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent)
                    } else {
                        ContactBody(c.id, c.phoneNumber, c.idMatcher, c.name, c.colors.color, c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent)
                    })
                } catch (e: Exception) {

                }
            }

            ApiUploadService.uploadContacts(contactBodies)
            Log.v(TAG, "uploaded contact changes: ${TimeUtils.now - startTime} ms")
        }.start()
    }
}
