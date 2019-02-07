package xyz.klinker.messenger.activity.share

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import xyz.klinker.messenger.activity.compose.ShareData
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.service.MessengerChooserTargetService
import xyz.klinker.messenger.shared.util.FileUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import java.io.File

class ShareIntentHandler(private val page: QuickSharePage) {

    fun handle(intent: Intent) {
        if (intent.action == null) {
            return
        }

        try {
            when {
                intent.action == Intent.ACTION_SENDTO -> shareDirectlyToSms(intent)
                intent.action == Intent.ACTION_SEND -> shareContent(intent)
            }
        } catch (e: Error) {
            AnalyticsHelper.caughtForceClose(page.context, "caught when sharing to quick share activity", e)
        }
    }

    private fun shareDirectlyToSms(intent: Intent) {
        if (intent.dataString == null) {
            return
        }

        val phoneNumbers = PhoneNumberUtils.parseAddress(Uri.decode(intent.dataString))

        val builder = StringBuilder()
        for (i in phoneNumbers.indices) {
            builder.append(PhoneNumberUtils.clearFormattingAndStripStandardReplacements(phoneNumbers[i]))
            if (i != phoneNumbers.size - 1) {
                builder.append(", ")
            }
        }

        val numbers = builder.toString()
        val data = intent.getStringExtra("sms_body")

        if (!numbers.isEmpty()) {
            page.setContacts(builder.toString().split(",".toRegex()).map { it.trim() })
        } else if (data != null) {
            page.setData(ShareData(MimeType.TEXT_PLAIN, data))
        }
    }

    private fun shareContent(intent: Intent) {
        val data = mutableListOf<ShareData>()

        if (intent.type == MimeType.TEXT_PLAIN && intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
            data.add(ShareData(MimeType.TEXT_PLAIN, intent.getStringExtra(Intent.EXTRA_TEXT)))
        } else if (intent.clipData != null) {
            var text = ""
            for (i in 0 until intent.clipData!!.itemCount) {
                text += intent.clipData!!.getItemAt(i).text
            }

            if (text != "null") {
                data.add(ShareData(MimeType.TEXT_PLAIN, text))
            }
        }

        if (intent.extras?.containsKey(Intent.EXTRA_STREAM) == true) {
            val imageData = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM).toString()
            val uri = try {
                val dst = File(page.activity.filesDir, (Math.random() * Integer.MAX_VALUE).toInt().toString() + "")
                val `in` = page.activity.contentResolver.openInputStream(Uri.parse(imageData))

                FileUtils.copy(`in`!!, dst)
                Uri.fromFile(dst).toString()
            } catch (e: Exception) {
                e.printStackTrace()
                imageData
            }

            data.add(ShareData(MimeType.IMAGE_PNG, uri))
        }

        if (intent.extras != null && intent.extras!!.containsKey(MessengerChooserTargetService.EXTRA_CONVO_ID)) {
            val conversationId = intent.extras!!.getLong(MessengerChooserTargetService.EXTRA_CONVO_ID)
            val conversation = DataSource.getConversation(page.context, conversationId)
            if (conversation != null) {
                page.setContacts(conversation.phoneNumbers!!.split(",".toRegex()).map { it.trim() })
            }
        }

        page.setData(data)
    }

}