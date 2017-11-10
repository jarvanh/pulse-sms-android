/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log

import com.google.firebase.auth.FirebaseAuth

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.Executor

import xyz.klinker.messenger.api.implementation.firebase.FirebaseDownloadCallback
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.api.entity.BlacklistBody
import xyz.klinker.messenger.api.entity.ContactBody
import xyz.klinker.messenger.api.entity.ConversationBody
import xyz.klinker.messenger.api.entity.DraftBody
import xyz.klinker.messenger.api.entity.MessageBody
import xyz.klinker.messenger.api.entity.ScheduledMessageBody
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Blacklist
import xyz.klinker.messenger.shared.data.model.Contact
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Draft
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.model.ScheduledMessage
import xyz.klinker.messenger.encryption.EncryptionUtils
import xyz.klinker.messenger.shared.util.*
import xyz.klinker.messenger.shared.util.listener.DirectExecutor

class ApiDownloadService : Service() {

    private var encryptionUtils: EncryptionUtils? = null
    private var showNotification = true
    private var completedMediaDownloads = 0

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showNotification = intent?.getBooleanExtra(ARG_SHOW_NOTIFICATION, true) ?: true

        downloadData()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun downloadData() {
        val notification = NotificationCompat.Builder(this,
                NotificationUtils.STATUS_NOTIFICATIONS_CHANNEL_ID)
                .setContentTitle(getString(R.string.downloading_and_decrypting))
                .setSmallIcon(R.drawable.ic_download)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true)
                .build()

        if (showNotification) {
            startForeground(MESSAGE_DOWNLOAD_ID, notification)
        }

        Thread {
            IS_RUNNING = true

            encryptionUtils = Account.encryptor
            DataSource.beginTransaction(this)

            val startTime = System.currentTimeMillis()
            wipeDatabase()
            downloadMessages()
            downloadConversations()
            downloadBlacklists()
            downloadScheduledMessages()
            downloadDrafts()
            downloadContacts()
            Log.v(TAG, "time to download: " + (System.currentTimeMillis() - startTime) + " ms")

            sendBroadcast(Intent(ACTION_DOWNLOAD_FINISHED))
            NotificationManagerCompat.from(applicationContext).cancel(MESSAGE_DOWNLOAD_ID)
            DataSource.setTransactionSuccessful(this)
            DataSource.endTransaction(this)
            downloadMedia()

            IS_RUNNING = false
        }.start()
    }

    private fun wipeDatabase() {
        DataSource.clearTables(this)
    }

    private fun downloadMessages() {
        val startTime = System.currentTimeMillis()
        val messageList = ArrayList<Message>()

        var pageNumber = 1
        var nullCount = 0
        var noMessages = false

        do {
            val messages = try {
                ApiUtils.api.message()
                        .list(Account.accountId, null, MESSAGE_DOWNLOAD_PAGE_SIZE, messageList.size)
                        .execute().body()
            } catch (e: IOException) {
                emptyArray<MessageBody>()
            }

            if (messages != null) {
                if (messages.isEmpty()) {
                    noMessages = true
                }

                for (body in messages) {
                    val message = Message(body)

                    try {
                        message.decrypt(encryptionUtils!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    messageList.add(message)
                }
            } else {
                nullCount++
            }

            Log.v(TAG, messageList.size.toString() + " messages downloaded. " + pageNumber + " pages so far.")
            pageNumber++
        } while (messageList.size % MESSAGE_DOWNLOAD_PAGE_SIZE == 0 && !noMessages && nullCount < 5)

        if (messageList.size > 0) {
            DataSource.insertMessages(this, messageList, false)
            Log.v(TAG, messageList.size.toString() + " messages inserted in " + (System.currentTimeMillis() - startTime) + " ms with " + pageNumber + " pages")

            messageList.clear()
        } else {
            Log.v(TAG, "messages failed to insert")
        }
    }

    private fun downloadConversations() {
        val startTime = System.currentTimeMillis()
        val conversations = try {
            ApiUtils.api.conversation().list(Account.accountId).execute().body()
        } catch (e: IOException) {
            emptyArray<ConversationBody>()
        }

        if (conversations != null) {
            for (body in conversations) {
                val conversation = Conversation(body)

                try {
                    conversation.decrypt(encryptionUtils!!)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.v(TAG, "decryption error while downloading conversations. Retrying now.")

                    retryConversationDownloadFromBadDecryption()
                    return
                }

                conversation.imageUri = ContactUtils.findImageUri(conversation.phoneNumbers, this)

                val image = ImageUtils.getContactImage(conversation.imageUri, this)
                if (conversation.imageUri != null && image == null) {
                    conversation.imageUri = null
                } else if (conversation.imageUri != null) {
                    conversation.imageUri = conversation.imageUri!! + "/photo"
                }

                image?.recycle()

                DataSource.insertConversation(this, conversation, false)
            }

            Log.v(TAG, "conversations inserted in " + (System.currentTimeMillis() - startTime) + " ms")
        } else {
            Log.v(TAG, "conversations failed to insert")
        }
    }

    // a bit probably got misplaced? Lets retry. If it doesn't work still, just skip inserting
    // that conversation
    private fun retryConversationDownloadFromBadDecryption() {
        val startTime = System.currentTimeMillis()
        val conversations = try {
            ApiUtils.api.conversation().list(Account.accountId).execute().body()
        } catch (e: IOException) {
            emptyArray<ConversationBody>()
        }

        if (conversations != null) {
            conversations.map { Conversation(it) }
                    .forEach {
                        try {
                            it.decrypt(encryptionUtils!!)
                            it.imageUri = ContactUtils.findImageUri(it.phoneNumbers, this)

                            if (it.imageUri != null && ImageUtils.getContactImage(it.imageUri, this) == null) {
                                it.imageUri = null
                            } else if (it.imageUri != null) {
                                it.imageUri = it.imageUri!! + "/photo"
                            }

                            DataSource.insertConversation(this, it, false)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Log.v(TAG, "error inserting conversation due to encryption. conversation_id: " + it.id)
                        }
                    }

            Log.v(TAG, "conversations inserted in " + (System.currentTimeMillis() - startTime) + " ms")
        } else {
            Log.v(TAG, "conversations failed to insert")
        }
    }

    private fun downloadBlacklists() {
        val startTime = System.currentTimeMillis()
        val blacklists = try {
            ApiUtils.api.blacklist().list(Account.accountId).execute().body()
        } catch (e: Exception) {
            emptyArray<BlacklistBody>()
        }

        if (blacklists != null) {
            for (body in blacklists) {
                val blacklist = Blacklist(body)
                blacklist.decrypt(encryptionUtils!!)
                DataSource.insertBlacklist(this, blacklist, false)
            }

            Log.v(TAG, "blacklists inserted in " + (System.currentTimeMillis() - startTime) + " ms")
        } else {
            Log.v(TAG, "blacklists failed to insert")
        }
    }

    private fun downloadScheduledMessages() {
        val startTime = System.currentTimeMillis()
        val messages = try {
            ApiUtils.api.scheduled().list(Account.accountId).execute().body()
        } catch (e: IOException) {
            emptyArray<ScheduledMessageBody>()
        }

        if (messages != null) {
            for (body in messages) {
                val message = ScheduledMessage(body)
                message.decrypt(encryptionUtils!!)
                DataSource.insertScheduledMessage(this, message, false)
            }

            Log.v(TAG, "scheduled messages inserted in " + (System.currentTimeMillis() - startTime) + " ms")
        } else {
            Log.v(TAG, "scheduled messages failed to insert")
        }
    }

    private fun downloadDrafts() {
        val startTime = System.currentTimeMillis()
        val drafts = try {
            ApiUtils.api.draft().list(Account.accountId).execute().body()
        } catch (e: IOException) {
            emptyArray<DraftBody>()
        }

        if (drafts != null) {
            for (body in drafts) {
                val draft = Draft(body)
                draft.decrypt(encryptionUtils!!)
                DataSource.insertDraft(this, draft, false)
            }

            Log.v(TAG, "drafts inserted in " + (System.currentTimeMillis() - startTime) + " ms")
        } else {
            Log.v(TAG, "drafts failed to insert")
        }
    }

    private fun downloadContacts() {
        val startTime = System.currentTimeMillis()
        val contacts = try {
            ApiUtils.api.contact().list(Account.accountId).execute().body()
        } catch (e: IOException) {
            emptyArray<ContactBody>()
        }

        if (contacts != null) {
            val contactList = ArrayList<Contact>()

            for (body in contacts) {
                val contact = Contact(body)
                contact.decrypt(encryptionUtils!!)
                contactList.add(contact)
            }

            DataSource.insertContacts(this, contactList, null, false)

            Log.v(TAG, "contacts inserted in " + (System.currentTimeMillis() - startTime) + " ms")
        } else {
            Log.v(TAG, "contacts failed to insert")
        }
    }

    private fun downloadMedia() {
        val builder = NotificationCompat.Builder(this,
                NotificationUtils.STATUS_NOTIFICATIONS_CHANNEL_ID)
                .setContentTitle(getString(R.string.decrypting_and_downloading_media))
                .setSmallIcon(R.drawable.ic_download)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true)
        val manager = NotificationManagerCompat.from(this)

        if (showNotification) {
            startForeground(MESSAGE_DOWNLOAD_ID, builder.build())
        }

        val auth = FirebaseAuth.getInstance()
        try {
            auth.signInAnonymously()
                    .addOnSuccessListener { processMediaDownload(manager, builder) }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "failed to sign in to firebase", e)
                        finishMediaDownload(manager)
                    }
        } catch (e: Exception) {
            // android wear issue
            finishMediaDownload(manager)
        }
    }

    private fun processMediaDownload(manager: NotificationManagerCompat,
                                     builder: NotificationCompat.Builder) {
        ApiUtils.saveFirebaseFolderRef(Account.accountId)

        Thread {
            try {
                Thread.sleep((1000 * 60 * 5).toLong())
            } catch (e: InterruptedException) {
            }

            finishMediaDownload(manager)
        }.start()


        val media = DataSource.getFirebaseMediaMessages(this)
        if (media.moveToFirst()) {
            val mediaCount = if (media.count > MAX_MEDIA_DOWNLOADS)
                MAX_MEDIA_DOWNLOADS
            else
                media.count
            var processing = 0
            do {
                val message = Message()
                message.fillFromCursor(media)
                processing++

                val file = File(filesDir,
                        message.id.toString() + MimeType.getExtension(message.mimeType!!))

                Log.v(TAG, "started downloading " + message.id)

                ApiUtils.downloadFileFromFirebase(Account.accountId, file, message.id, encryptionUtils, FirebaseDownloadCallback {
                    completedMediaDownloads++

                    DataSource.updateMessageData(this@ApiDownloadService, message.id, Uri.fromFile(file).toString())
                    builder.setProgress(mediaCount, completedMediaDownloads, false)

                    if (completedMediaDownloads >= mediaCount) {
                        finishMediaDownload(manager)
                    } else if (showNotification) {
                        startForeground(MESSAGE_DOWNLOAD_ID, builder.build())
                    }
                }, 0)
            } while (media.moveToNext() && processing < MAX_MEDIA_DOWNLOADS)
        }

        media.closeSilent()
    }

    private fun finishMediaDownload(manager: NotificationManagerCompat) {
        stopForeground(true)
        stopSelf()
    }

    companion object {

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, ApiDownloadService::class.java))
            } else {
                context.startService(Intent(context, ApiDownloadService::class.java))
            }
        }

        private val TAG = "ApiDownloadService"
        private val MESSAGE_DOWNLOAD_ID = 7237
        val ACTION_DOWNLOAD_FINISHED = "xyz.klinker.messenger.API_DOWNLOAD_FINISHED"

        val MESSAGE_DOWNLOAD_PAGE_SIZE = 300
        val MAX_MEDIA_DOWNLOADS = 75
        val ARG_SHOW_NOTIFICATION = "show_notification"

        var IS_RUNNING = false
    }

}
