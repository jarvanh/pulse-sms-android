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

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.util.Log

import com.google.firebase.auth.FirebaseAuth

import java.io.IOException
import java.util.ArrayList

import retrofit2.Response
import xyz.klinker.messenger.api.entity.*
import xyz.klinker.messenger.api.implementation.LoginActivity
import xyz.klinker.messenger.api.implementation.firebase.FirebaseUploadCallback
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.encryption.EncryptionUtils
import xyz.klinker.messenger.api.implementation.BinaryUtils
import xyz.klinker.messenger.shared.data.model.*
import xyz.klinker.messenger.shared.util.*

open class ApiUploadService : Service() {

    private var encryptionUtils: EncryptionUtils? = null
    private var completedMediaUploads = 0
    private var finished = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        uploadData()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun uploadData() {
        val notification = NotificationCompat.Builder(this, NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
                .setContentTitle(getString(R.string.encrypting_and_uploading))
                .setSmallIcon(R.drawable.ic_upload)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true)
                .build()
        
        startForeground(MESSAGE_UPLOAD_ID, notification)

        encryptionUtils = Account.encryptor
        if (encryptionUtils == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            return
        }
        
        Thread {
            val startTime = TimeUtils.now

            uploadMessages()
            uploadConversations()
            uploadContacts(this, encryptionUtils!!)
            uploadBlacklists()
            uploadScheduledMessages()
            uploadDrafts()
            uploadTemplates()
            uploadFolders()
            uploadAutoReplies()

            Log.v(TAG, "time to upload: " + (TimeUtils.now - startTime) + " ms")

            uploadMedia()
        }.start()
    }

    private fun uploadMessages() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getMessages(this)

        if (cursor.moveToFirst()) {
            val messages = ArrayList<MessageBody>()
            var firebaseNumber = 0

            do {
                val m = Message()
                m.fillFromCursor(cursor)

                // instead of sending the URI, we'll upload these images to firebase and retrieve
                // them on another device based on account id and message id.
                if (m.mimeType != MimeType.TEXT_PLAIN) {
                    m.data = "firebase " + firebaseNumber
                    firebaseNumber++
                }

                m.encrypt(encryptionUtils!!)
                val message = MessageBody(m.id, m.conversationId, m.type, m.data,
                        m.timestamp, m.mimeType, m.read, m.seen, m.from, m.color, "-1", m.simPhoneNumber)
                messages.add(message)
            } while (cursor.moveToNext())

            var successPages = 0
            var expectedPages = 0
            val pages = PaginationUtils.getPages(messages, MESSAGE_UPLOAD_PAGE_SIZE)

            for (page in pages) {
                val request = AddMessagesRequest(Account.accountId, page.toTypedArray())
                try {
                    val response = ApiUtils.api.message().add(request).execute()
                    expectedPages++
                    if (ApiUtils.isCallSuccessful(response)) {
                        successPages++
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                Log.v(TAG, "uploaded " + page.size + " messages for page " + expectedPages)
            }

            if (successPages != expectedPages) {
                Log.v(TAG, "failed to upload messages in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "messages upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadConversations() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getAllConversations(this)

        if (cursor.moveToFirst()) {
            val conversations = arrayOfNulls<ConversationBody>(cursor.count)

            do {
                val c = Conversation()
                c.fillFromCursor(cursor)
                c.encrypt(encryptionUtils!!)
                val conversation = ConversationBody(c.id, c.colors.color,
                        c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent, c.ledColor, c.pinned,
                        c.read, c.timestamp, c.title, c.phoneNumbers, c.snippet, c.ringtoneUri, null,
                        c.idMatcher, c.mute, c.archive, c.private, c.folderId)/*c.imageUri*/

                conversations[cursor.position] = conversation
            } while (cursor.moveToNext())

            val request = AddConversationRequest(Account.accountId, conversations)
            var result: Response<*>?

            var errorText: String? = null
            try {
                result = ApiUtils.api.conversation().add(request).execute()
            } catch (e: IOException) {
                try {
                    result = ApiUtils.api.conversation().add(request).execute()
                } catch (x: Exception) {
                    errorText = e.message
                    e.printStackTrace()
                    result = null
                }

            }

            if (result == null || !ApiUtils.isCallSuccessful(result)) {
                Log.v(TAG, "failed to upload conversations in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, result.toString())
                Log.v(TAG, "conversations upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadBlacklists() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getBlacklists(this)

        if (cursor.moveToFirst()) {
            val blacklists = arrayOfNulls<BlacklistBody>(cursor.count)

            do {
                val b = Blacklist()
                b.fillFromCursor(cursor)
                b.encrypt(encryptionUtils!!)
                val blacklist = BlacklistBody(b.id, b.phoneNumber)

                blacklists[cursor.position] = blacklist
            } while (cursor.moveToNext())

            val request = AddBlacklistRequest(Account.accountId, blacklists)
            val result = try {
                ApiUtils.api.blacklist().add(request).execute()
            } catch (e: IOException) {
                null
            }

            if (result == null || !ApiUtils.isCallSuccessful(result)) {
                Log.v(TAG, "failed to upload blacklists in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "blacklists upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadScheduledMessages() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getScheduledMessages(this)

        if (cursor.moveToFirst()) {
            val messages = arrayOfNulls<ScheduledMessageBody>(cursor.count)

            do {
                val m = ScheduledMessage()
                m.fillFromCursor(cursor)
                m.encrypt(encryptionUtils!!)
                val message = ScheduledMessageBody(m.id, m.to, m.data,
                        m.mimeType, m.timestamp, m.title)

                messages[cursor.position] = message
            } while (cursor.moveToNext())

            val request = AddScheduledMessageRequest(Account.accountId, messages)
            val result = try {
                ApiUtils.api.scheduled().add(request).execute()
            } catch (e: IOException) {
                null
            }

            if (result == null || !ApiUtils.isCallSuccessful(result)) {
                Log.v(TAG, "failed to upload scheduled messages in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "scheduled messages upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadDrafts() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getDrafts(this)

        if (cursor.moveToFirst()) {
            val drafts = arrayOfNulls<DraftBody>(cursor.count)

            do {
                val d = Draft()
                d.fillFromCursor(cursor)
                d.encrypt(encryptionUtils!!)
                val draft = DraftBody(d.id, d.conversationId, d.data, d.mimeType)

                drafts[cursor.position] = draft
            } while (cursor.moveToNext())

            val request = AddDraftRequest(Account.accountId, drafts)
            val result = try {
                ApiUtils.api.draft().add(request).execute().body()
            } catch (e: IOException) {
                null
            }

            if (result == null) {
                Log.v(TAG, "failed to upload drafts in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "drafts upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadTemplates() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getTemplates(this)

        if (cursor.moveToFirst()) {
            val templates = arrayOfNulls<TemplateBody>(cursor.count)

            do {
                val t = Template()
                t.fillFromCursor(cursor)
                t.encrypt(encryptionUtils!!)
                val template = TemplateBody(t.id, t.text)

                templates[cursor.position] = template
            } while (cursor.moveToNext())

            val request = AddTemplateRequest(Account.accountId, templates)
            val result = try {
                ApiUtils.api.template().add(request).execute().body()
            } catch (e: IOException) {
                null
            }

            if (result == null) {
                Log.v(TAG, "failed to upload templates in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "template upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadFolders() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getFolders(this)

        if (cursor.moveToFirst()) {
            val folders = arrayOfNulls<FolderBody>(cursor.count)

            do {
                val f = Folder()
                f.fillFromCursor(cursor)
                f.encrypt(encryptionUtils!!)
                val folder = FolderBody(f.id, f.name, f.colors.color, f.colors.colorDark, f.colors.colorLight, f.colors.colorAccent)

                folders[cursor.position] = folder
            } while (cursor.moveToNext())

            val request = AddFolderRequest(Account.accountId, folders)
            val result = try {
                ApiUtils.api.folder().add(request).execute().body()
            } catch (e: IOException) {
                null
            }

            if (result == null) {
                Log.v(TAG, "failed to upload folders in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "folder upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    private fun uploadAutoReplies() {
        val startTime = TimeUtils.now
        val cursor = DataSource.getAutoReplies(this)

        if (cursor.moveToFirst()) {
            val replies = arrayOfNulls<AutoReplyBody>(cursor.count)

            do {
                val r = AutoReply()
                r.fillFromCursor(cursor)
                r.encrypt(encryptionUtils!!)
                val reply = AutoReplyBody(r.id, r.type, r.pattern, r.response)

                replies[cursor.position] = reply
            } while (cursor.moveToNext())

            val request = AddAutoReplyRequest(Account.accountId, replies)
            val result = try {
                ApiUtils.api.autoReply().add(request).execute().body()
            } catch (e: IOException) {
                null
            }

            if (result == null) {
                Log.v(TAG, "failed to upload auto replies in " +
                        (TimeUtils.now - startTime) + " ms")
            } else {
                Log.v(TAG, "auto reply upload successful in " +
                        (TimeUtils.now - startTime) + " ms")
            }
        }

        cursor.closeSilent()
    }

    /**
     * Media will be uploaded after the messages finish uploading
     */
    private fun uploadMedia() {
        val builder = NotificationCompat.Builder(this, NotificationUtils.ACCOUNT_ACTIVITY_CHANNEL_ID)
                .setContentTitle(getString(R.string.encrypting_and_uploading_media))
                .setSmallIcon(R.drawable.ic_upload)
                .setProgress(0, 0, true)
                .setLocalOnly(true)
                .setColor(ColorSet.DEFAULT(this).color)
                .setOngoing(true)
        val manager = NotificationManagerCompat.from(this)
        startForeground(MESSAGE_UPLOAD_ID, builder.build())

        val auth = FirebaseAuth.getInstance()
        auth.signInAnonymously()
                .addOnSuccessListener { processMediaUpload(manager, builder) }
                .addOnFailureListener { e ->
                    Log.e(TAG, "failed to sign in to firebase", e)
                    finishMediaUpload(manager)
                }
    }

    private fun processMediaUpload(manager: NotificationManagerCompat,
                                   builder: NotificationCompat.Builder) {
        ApiUtils.saveFirebaseFolderRef(Account.accountId)

        Thread {
            try {
                Thread.sleep((1000 * 60 * 2).toLong())
            } catch (e: InterruptedException) {
            }

            finishMediaUpload(manager)
        }.start()

        val media = DataSource.getAllMediaMessages(this, NUM_MEDIA_TO_UPLOAD)
        if (media.moveToFirst()) {
            val mediaCount = if (media.count < NUM_MEDIA_TO_UPLOAD) media.count else NUM_MEDIA_TO_UPLOAD
            do {
                val message = Message()
                message.fillFromCursor(media)

                Log.v(TAG, "started uploading " + message.id)

                val bytes = BinaryUtils.getMediaBytes(this, message.data, message.mimeType, true)

                ApiUtils.uploadBytesToFirebase(Account.accountId, bytes, message.id, encryptionUtils, FirebaseUploadCallback {
                    completedMediaUploads++

                    builder.setProgress(mediaCount, completedMediaUploads, false)
                    builder.setContentTitle(getString(R.string.encrypting_and_uploading_count,
                            completedMediaUploads + 1, media.count))

                    if (completedMediaUploads >= mediaCount) {
                        finishMediaUpload(manager)
                    } else if (!finished) {
                        startForeground(MESSAGE_UPLOAD_ID, builder.build())
                    }
                }, 0)
            } while (media.moveToNext())

            if (mediaCount == 0) {
                finishMediaUpload(manager)
            }
        } else {
            finishMediaUpload(manager)
        }

        media.closeSilent()
    }

    private fun finishMediaUpload(manager: NotificationManagerCompat) {
        stopForeground(true)
        stopSelf()
        finished = true
    }

    companion object {

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, ApiUploadService::class.java))
            } else {
                context.startService(Intent(context, ApiUploadService::class.java))
            }
        }

        private val TAG = "ApiUploadService"
        private val MESSAGE_UPLOAD_ID = 7235
        val NUM_MEDIA_TO_UPLOAD = 20

        val MESSAGE_UPLOAD_PAGE_SIZE = 300

        public fun uploadContacts(context: Context, encryptionUtils: EncryptionUtils) {
            val startTime = TimeUtils.now
            val cursor = DataSource.getContacts(context)

            if (cursor.moveToFirst()) {
                val contacts = ArrayList<ContactBody>()

                do {
                    val c = Contact()
                    c.fillFromCursor(cursor)
                    c.encrypt(encryptionUtils)
                    val contact = ContactBody(c.id, c.phoneNumber, c.idMatcher, c.name, c.colors.color,
                            c.colors.colorDark, c.colors.colorLight, c.colors.colorAccent)
                    contacts.add(contact)
                } while (cursor.moveToNext())

                var successPages = 0
                var expectedPages = 0
                val pages = PaginationUtils.getPages(contacts, MESSAGE_UPLOAD_PAGE_SIZE)

                for (page in pages) {
                    val request = AddContactRequest(Account.accountId, page.toTypedArray())
                    try {
                        val response = ApiUtils.api.contact().add(request).execute()
                        expectedPages++
                        if (ApiUtils.isCallSuccessful(response)) {
                            successPages++
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    Log.v(TAG, "uploaded " + page.size + " contacts for page " + expectedPages)
                }

                if (successPages != expectedPages) {
                    Log.v(TAG, "failed to upload contacts in " +
                            (TimeUtils.now - startTime) + " ms")
                } else {
                    Log.v(TAG, "contacts upload successful in " +
                            (TimeUtils.now - startTime) + " ms")
                }
            }

            cursor.closeSilent()
        }
    }
}
