/*
 * Copyright (C) 2020 Luke Klinker
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

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log

import com.google.firebase.auth.FirebaseAuth
import xyz.klinker.messenger.api.entity.*

import java.io.File
import java.io.IOException

import xyz.klinker.messenger.api.implementation.firebase.FirebaseDownloadCallback
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.encryption.EncryptionUtils
import xyz.klinker.messenger.shared.data.model.*
import xyz.klinker.messenger.shared.util.*

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
                NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
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

            val startTime = TimeUtils.now
            wipeDatabase()

            downloadMessages()
            downloadConversations()
            downloadBlacklists()
            downloadScheduledMessages()
            downloadDrafts()
            downloadContacts()
            downloadTemplates()
            downloadFolders()
            downloadAutoReplies()

            ensureMessages()

            Log.v(TAG, "time to download: " + (TimeUtils.now - startTime) + " ms")

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
        val startTime = TimeUtils.now
        val messageList = mutableListOf<Message>()

        var pageNumber = 1
        var downloaded = 0
        var nullCount = 0

        do {
            val messages = try {
                ApiUtils.api.message()
                        .list(Account.accountId, null, MESSAGE_DOWNLOAD_PAGE_SIZE, downloaded)
                        .execute().body()
            } catch (e: IOException) {
                emptyArray<MessageBody>()
            }

            if (messages != null && !messages.isEmpty()) {
                for (body in messages) {
                    val message = Message(body)

                    try {
                        message.decrypt(encryptionUtils!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    messageList.add(message)
                }

                DataSource.insertMessages(this, messageList, false)
                downloaded += messageList.size

                messageList.clear()
            } else {
                nullCount++

                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                }
            }

            Log.v(TAG, downloaded.toString() + " messages downloaded. " + pageNumber + " pages so far.")
            pageNumber++
        } while (downloaded % MESSAGE_DOWNLOAD_PAGE_SIZE == 0 && nullCount < 5)

        if (downloaded > 0) {
            Log.v(TAG, downloaded.toString() + " messages inserted in " + (TimeUtils.now - startTime) + " ms with " + pageNumber + " pages")
        } else {
            Log.v(TAG, "messages failed to insert")
        }
    }

    private fun downloadConversations() {
        val startTime = TimeUtils.now
        val conversationList = mutableListOf<Conversation>()

        var pageNumber = 1
        var downloaded = 0
        var nullCount = 0

        do {
            val conversations = try {
                ApiUtils.api.conversation()
                        .list(Account.accountId, CONVERSATION_DOWNLOAD_PAGE_SIZE, downloaded)
                        .execute().body()
            } catch (e: IOException) {
                emptyArray<ConversationBody>()
            }

            if (conversations != null && !conversations.isEmpty()) {
                for (body in conversations) {
                    val conversation = Conversation(body)

                    try {
                        conversation.decrypt(encryptionUtils!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    conversation.imageUri = ContactUtils.findImageUri(conversation.phoneNumbers, this)
                    conversationList.add(conversation)
                }

                DataSource.insertRawConversations(conversationList, this)
                downloaded += conversationList.size

                conversationList.clear()
            } else {
                nullCount++

                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                }
            }

            Log.v(TAG, downloaded.toString() + " conversations downloaded. " + pageNumber + " pages so far.")
            pageNumber++
        } while (downloaded % CONVERSATION_DOWNLOAD_PAGE_SIZE == 0 && nullCount < 5)

        if (downloaded > 0) {
            Log.v(TAG, downloaded.toString() + " conversations inserted in " + (TimeUtils.now - startTime) + " ms")
        } else {
            Log.v(TAG, "contacts failed to insert")
        }
    }

    private fun downloadBlacklists() {
        val startTime = TimeUtils.now
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

            Log.v(TAG, "blacklists inserted in " + (TimeUtils.now - startTime) + " ms")
        } else {
            Log.v(TAG, "blacklists failed to insert")
        }
    }

    private fun downloadScheduledMessages() {
        val startTime = TimeUtils.now
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

            Log.v(TAG, "scheduled messages inserted in " + (TimeUtils.now - startTime) + " ms")
        } else {
            Log.v(TAG, "scheduled messages failed to insert")
        }
    }

    private fun downloadDrafts() {
        val startTime = TimeUtils.now
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

            Log.v(TAG, "drafts inserted in " + (TimeUtils.now - startTime) + " ms")
        } else {
            Log.v(TAG, "drafts failed to insert")
        }
    }

    private fun downloadContacts() {
        val startTime = TimeUtils.now
        val contactList = mutableListOf<Contact>()

        var pageNumber = 1
        var downloaded = 0
        var nullCount = 0

        do {
            val contacts = try {
                ApiUtils.api.contact()
                        .list(Account.accountId, CONTACTS_DOWNLOAD_PAGE_SIZE, downloaded)
                        .execute().body()
            } catch (e: IOException) {
                emptyArray<ContactBody>()
            }

            if (contacts != null && !contacts.isEmpty()) {
                for (body in contacts) {
                    val contact = Contact(body)

                    try {
                        contact.decrypt(encryptionUtils!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    contactList.add(contact)
                }

                DataSource.insertContacts(this, contactList, null, false)
                downloaded += contactList.size

                contactList.clear()
            } else {
                nullCount++

                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                }
            }

            Log.v(TAG, downloaded.toString() + " contacts downloaded. " + pageNumber + " pages so far.")
            pageNumber++
        } while (downloaded % CONTACTS_DOWNLOAD_PAGE_SIZE == 0 && nullCount < 5)

        if (downloaded > 0) {
            Log.v(TAG, downloaded.toString() + " contacts inserted in " + (TimeUtils.now - startTime) + " ms")
        } else {
            Log.v(TAG, "contacts failed to insert")
        }
    }

    private fun downloadTemplates() {
        val startTime = TimeUtils.now
        val templates = try {
            ApiUtils.api.template().list(Account.accountId).execute().body()
        } catch (e: IOException) {
            emptyArray<TemplateBody>()
        }

        if (templates != null) {
            for (body in templates) {
                val template = Template(body)
                template.decrypt(encryptionUtils!!)
                DataSource.insertTemplate(this, template, false)
            }

            Log.v(TAG, "templates inserted in " + (TimeUtils.now - startTime) + " ms")
        } else {
            Log.v(TAG, "templates failed to insert")
        }
    }

    private fun downloadFolders() {
        val startTime = TimeUtils.now
        val folders = try {
            ApiUtils.api.folder().list(Account.accountId).execute().body()
        } catch (e: IOException) {
            emptyArray<FolderBody>()
        }

        if (folders != null) {
            for (body in folders) {
                val folder = Folder(body)
                folder.decrypt(encryptionUtils!!)
                DataSource.insertFolder(this, folder, false)
            }

            Log.v(TAG, "folders inserted in " + (TimeUtils.now - startTime) + " ms")
        } else {
            Log.v(TAG, "folders failed to insert")
        }
    }

    private fun downloadAutoReplies() {
        val startTime = TimeUtils.now
        val replies = try {
            ApiUtils.api.autoReply().list(Account.accountId).execute().body()
        } catch (e: IOException) {
            emptyArray<AutoReplyBody>()
        }

        if (replies != null) {
            for (body in replies) {
                val reply = AutoReply(body)
                reply.decrypt(encryptionUtils!!)
                DataSource.insertAutoReply(this, reply, false)
            }

            Log.v(TAG, "auto replies inserted in " + (TimeUtils.now - startTime) + " ms")
        } else {
            Log.v(TAG, "auto replies failed to insert")
        }
    }

    private fun downloadMedia() {
        val builder = NotificationCompat.Builder(this,
                NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
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

        try {
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

                if (mediaCount == 0) {
                    finishMediaDownload(manager)
                }
            } else {
                finishMediaDownload(manager)
            }

            media.closeSilent()
        } catch (e: Exception) {
            finishMediaDownload(manager)
        }

    }

    private fun finishMediaDownload(manager: NotificationManagerCompat) {
        stopForeground(true)
        stopSelf()
    }

    private fun ensureMessages() {
        // some people say that after a download, they have their full conversation list, but all the conversations are empty.
        // so, if they have conversations in the database and no messages, we should re-download the messages.
        if (DataSource.getConversationCount(this) > 0 && DataSource.getMessageCount(this) == 0) {
            downloadMessages()
        }
    }

    companion object {
        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, ApiDownloadService::class.java))
            } else {
                context.startService(Intent(context, ApiDownloadService::class.java))
            }
        }

        private const val TAG = "ApiDownloadService"
        private const val MESSAGE_DOWNLOAD_ID = 7237
        const val ACTION_DOWNLOAD_FINISHED = "xyz.klinker.messenger.API_DOWNLOAD_FINISHED"

        const val MESSAGE_DOWNLOAD_PAGE_SIZE = 5000
        const val CONVERSATION_DOWNLOAD_PAGE_SIZE = 500
        const val CONTACTS_DOWNLOAD_PAGE_SIZE = 1000
        const val MAX_MEDIA_DOWNLOADS = 400
        const val ARG_SHOW_NOTIFICATION = "show_notification"

        var IS_RUNNING = false
    }

}
