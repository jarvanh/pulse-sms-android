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

package xyz.klinker.messenger.shared.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager
import android.util.Log
import com.klinker.android.send_message.Message
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.receiver.MmsSentReceiver
import xyz.klinker.messenger.shared.receiver.SmsDeliveredReceiver
import xyz.klinker.messenger.shared.receiver.SmsSentReceiver
import xyz.klinker.messenger.shared.receiver.SmsSentReceiverNoRetry
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

/**
 * Utility for helping to send messages.
 */
class SendUtils @JvmOverloads constructor(private val subscriptionId: Int? = null) {
    private var forceNoSignature = false
    private var forceSplitMessage = false
    private var retryOnFailedMessages = true

    fun setForceNoSignature(forceNoSignature: Boolean): SendUtils {
        this.forceNoSignature = forceNoSignature
        return this
    }

    fun setForceSplitMessage(splitMessage: Boolean): SendUtils {
        this.forceSplitMessage = splitMessage
        return this
    }

    fun setRetryFailedMessages(retry: Boolean): SendUtils {
        this.retryOnFailedMessages = retry
        return this
    }

    @JvmOverloads
    fun send(context: Context, text: String, addresses: String, data: Uri? = null, mimeType: String? = null): Uri? {
        return send(context, text, addresses.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray(), data, mimeType)
    }

    @JvmOverloads
    fun send(context: Context, text: String, addresses: Array<String>, data: Uri? = null, mimeType: String? = null): Uri? {
        var text = text
        var data = data
        var mimeType = mimeType

        if (FeatureFlags.NEVER_SEND_FROM_WATCH && WearableCheck.isAndroidWear(context)) {
            return data
        }

        val appSettings = xyz.klinker.messenger.shared.data.Settings
        if (!appSettings.signature!!.isEmpty() && !forceNoSignature) {
            text += "\n" + appSettings.signature!!
        }

        val settings = Settings()
        settings.deliveryReports = appSettings.deliveryReports
        settings.sendLongAsMms = MmsSettings.convertLongMessagesToMMS
        settings.sendLongAsMmsAfter = MmsSettings.numberOfMessagesBeforeMms
        settings.group = MmsSettings.groupMMS
        settings.stripUnicode = appSettings.stripUnicode
        settings.preText = if (appSettings.giffgaffDeliveryReports) "*0#" else ""
        settings.split = forceSplitMessage || shouldSplitMessages(context)

        if (MmsSettings.overrideSystemAPN) {
            settings.useSystemSending = false

            settings.mmsc = MmsSettings.mmscUrl
            settings.proxy = MmsSettings.mmsProxy
            settings.port = MmsSettings.mmsPort
            settings.agent = MmsSettings.userAgent
            settings.userProfileUrl = MmsSettings.userAgentProfileUrl
            settings.uaProfTagName = MmsSettings.userAgentProfileTagName
        }

        if (subscriptionId != null && subscriptionId != 0 && subscriptionId != -1) {
            settings.subscriptionId = subscriptionId
        }

        val transaction = Transaction(context, settings)
        transaction.setExplicitBroadcastForDeliveredSms(Intent(context, SmsDeliveredReceiver::class.java))
        transaction.setExplicitBroadcastForSentSms(Intent(context, if (retryOnFailedMessages) SmsSentReceiver::class.java else SmsSentReceiverNoRetry::class.java))
        transaction.setExplicitBroadcastForSentMms(Intent(context, MmsSentReceiver::class.java))

        val message = Message(text, addresses)

        if (data != null) {
            try {
                Log.v("Sending MMS", "mime type: " + mimeType!!)
                if (MimeType.isStaticImage(mimeType)) {
                    data = ImageUtils.scaleToSend(context, data, mimeType)

                    if (mimeType != MimeType.IMAGE_PNG) {
                        mimeType = MimeType.IMAGE_JPEG
                    }
                }

                val bytes = getBytes(context, data!!)
                Log.v("Sending MMS", "size: " + bytes.size + " bytes, mime type: " + mimeType)
                message.addMedia(bytes, mimeType)
            } catch (e: NullPointerException) {
                Log.e("Sending Exception", "Could not attach media: " + data, e)
            } catch (e: IOException) {
                Log.e("Sending Exception", "Could not attach media: " + data, e)
            } catch (e: SecurityException) {
                Log.e("Sending Exception", "Could not attach media: " + data, e)
            }

        }

        if (!Account.exists() || Account.primary) {
            try {
                transaction.sendNewMessage(message, Transaction.NO_THREAD_ID)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            } catch (e: UnsupportedOperationException) {
                // Sent from a Chromebook? How did they get to this point?
                e.printStackTrace()
            }

        }

        return data
    }

    fun shouldSplitMessages(context: Context): Boolean {
        val carrierDoesntAutoSplit = Arrays.asList("u.s. cellular")

        try {
            val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val carrierName = manager.networkOperatorName
            if (carrierDoesntAutoSplit.contains(carrierName.toLowerCase())) {
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    companion object {
        @Throws(IOException::class, NullPointerException::class, SecurityException::class)
        fun getBytes(context: Context, data: Uri): ByteArray {
            val stream = context.contentResolver.openInputStream(data)
            val byteBuffer = ByteArrayOutputStream()
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)

            var len = stream!!.read(buffer)
            while (len != -1) {
                byteBuffer.write(buffer, 0, len)
                len = stream.read(buffer)
            }

            stream.closeSilent()

            return byteBuffer.toByteArray()
        }
    }
}
