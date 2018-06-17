package xyz.klinker.messenger.activity.compose

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Parcelable
import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.service.MessengerChooserTargetService
import xyz.klinker.messenger.shared.util.FileUtils
import xyz.klinker.messenger.shared.util.NonStandardUriUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils
import xyz.klinker.messenger.shared.util.TimeUtils
import java.io.File
import java.net.URLDecoder

@Suppress("DEPRECATION")
class ComposeIntentHandler(private val activity: ComposeActivity) {

    fun handle(intent: Intent) {
        if (intent.action == null) {
            return
        }

        try {
            when {
                intent.action == ComposeConstants.ACTION_EDIT_RECIPIENTS -> changeGroupMessageParticipants(intent)
                intent.action == Intent.ACTION_SENDTO -> shareDirectlyToSms(intent)
                intent.action == Intent.ACTION_VIEW && (intent.type == null || intent.type == MimeType.TEXT_PLAIN) -> viewIntent(intent)
                intent.action == Intent.ACTION_SEND -> shareContent(intent)
            }
        } catch (e: Exception) {
            AnalyticsHelper.caughtForceClose(activity, "caught when sharing to compose activity", e)
        }
    }

    private fun changeGroupMessageParticipants(intent: Intent) {
        val phoneNumbers = intent.getStringExtra(ComposeConstants.EXTRA_EDIT_RECIPIENTS_NUMBERS)
        val title = intent.getStringExtra(ComposeConstants.EXTRA_EDIT_RECIPIENTS_TITLE)
        Handler().post({ activity.contactsProvider.onClicked(title, phoneNumbers, null) })
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
            activity.sender.showConversation(builder.toString(), data)
        } else if (data != null) {
            activity.sender.resetViews(data, MimeType.TEXT_PLAIN, false)
        }
    }

    private fun viewIntent(intent: Intent) {
        if (intent.extras != null && intent.extras.containsKey("sms_body")) {
            val body = intent.extras.getString("sms_body")
            if (intent.dataString != null) {
                val phoneNumbers = if (intent.dataString.contains("?")) {
                    PhoneNumberUtils.parseAddress(Uri.decode(intent.dataString.substring(0, intent.dataString.indexOf("?"))))
                } else {
                    PhoneNumberUtils.parseAddress(Uri.decode(intent.dataString))
                }

                val builder = StringBuilder()
                for (i in phoneNumbers.indices) {
                    builder.append(PhoneNumberUtils.clearFormattingAndStripStandardReplacements(phoneNumbers[i]))
                    if (i != phoneNumbers.size - 1) {
                        builder.append(", ")
                    }
                }

                val numbers = builder.toString()
                activity.shareHandler.apply(MimeType.TEXT_PLAIN, body, numbers)
            } else {
                activity.sender.resetViews(body, MimeType.TEXT_PLAIN, false)
            }
        } else if (intent.dataString != null) {
            initiatedFromWebLink(intent)
        }
    }

    private fun initiatedFromWebLink(intent: Intent) {
        val phoneNumbers = if (intent.dataString.contains("?")) {
            PhoneNumberUtils.parseAddress(Uri.decode(intent.dataString.substring(0, intent.dataString.indexOf("?"))))
        } else {
            PhoneNumberUtils.parseAddress(Uri.decode(intent.dataString))
        }

        val builder = StringBuilder()
        for (i in phoneNumbers.indices) {
            builder.append(PhoneNumberUtils.clearFormattingAndStripStandardReplacements(phoneNumbers[i]))
            if (i != phoneNumbers.size - 1) {
                builder.append(", ")
            }
        }

        val numbers = builder.toString()

        val body = NonStandardUriUtils.getQueryParams(intent.dataString)["body"]
        if (body != null) {
            activity.shareHandler.apply(MimeType.TEXT_PLAIN, body, numbers)
        } else {
            activity.sender.showConversation(numbers)
        }
    }

    private fun shareContent(intent: Intent) {
        var data: String? = ""
        var image = false

        if (intent.type == MimeType.TEXT_PLAIN) {
            data = intent.getStringExtra(Intent.EXTRA_TEXT)
        } else if (MimeType.isVcard(intent.type)) {
            shareVCard(intent)
            return
        } else if (intent.clipData != null) {
            for (i in 0 until intent.clipData.itemCount) {
                data += intent.clipData.getItemAt(i).text
            }

            if (data == "null") {
                image = true
            } else {
                intent.type = MimeType.TEXT_PLAIN
            }
        } else {
            image = true
        }

        if (image) {
            val tempData = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM).toString()
            data = try {
                val dst = File(activity.filesDir, (Math.random() * Integer.MAX_VALUE).toInt().toString() + "")
                val `in` = activity.contentResolver.openInputStream(Uri.parse(tempData))

                FileUtils.copy(`in`, dst)
                Uri.fromFile(dst).toString()
            } catch (e: Exception) {
                e.printStackTrace()
                tempData
            }
        }

        if (intent.extras != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && intent.extras.containsKey(MessengerChooserTargetService.EXTRA_CONVO_ID)) {
            activity.shareHandler.directShare(data, intent.type)
        } else if (intent.extras.containsKey(Intent.EXTRA_PHONE_NUMBER)) {
            val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            activity.shareHandler.apply(intent.type, data, number)
        } else {
            activity.sender.resetViews(if (data == null) "" else data, intent.type)
        }
    }

    private fun shareVCard(intent: Intent) {
        val data = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM).toString()

        try {
            if (intent.extras != null && intent.extras.containsKey(MessengerChooserTargetService.EXTRA_CONVO_ID)) {
                activity.shareHandler.directShare(data, intent.type, true)
            } else {
                activity.sender.fab.setImageResource(R.drawable.ic_send)
                activity.sender.resetViews(data, intent.type, true)
            }
        } catch (e: NoClassDefFoundError) {
            activity.sender.fab.setImageResource(R.drawable.ic_send)
            activity.sender.resetViews(data, intent.type, true)
        }
    }
}