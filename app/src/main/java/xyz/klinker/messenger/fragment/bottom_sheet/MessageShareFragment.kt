package xyz.klinker.messenger.fragment.bottom_sheet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast

import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.compose.ComposeActivity
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.utils.multi_select.MessageMultiSelectDelegate

import android.content.Context.CLIPBOARD_SERVICE
import androidx.fragment.app.FragmentActivity
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Conversation

class MessageShareFragment : TabletOptimizedBottomSheetDialogFragment() {

    private val fragmentActivity: FragmentActivity? by lazy { activity }

    private var conversation: Conversation? = null
    private var messages: List<Message?>? = null

    override fun createLayout(inflater: LayoutInflater): View {
        val contentView = View.inflate(context, R.layout.bottom_sheet_share, null)

        if (fragmentActivity == null) {
            return contentView
        }

        val shareExternal = contentView.findViewById<View>(R.id.share_external)
        val forwardToContact = contentView.findViewById<View>(R.id.forward_to_contact)
        val copyText = contentView.findViewById<View>(R.id.copy_text)

        shareExternal.setOnClickListener {
            val activity = activity ?: return@setOnClickListener

            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_TEXT, getTextToSend())
            shareIntent.type = getMimeType()
            fragmentActivity?.startActivity(Intent.createChooser(shareIntent,
                    activity.resources.getText(R.string.share_content)))

            dismiss()
        }

        forwardToContact.setOnClickListener {
            val shareIntent = Intent(fragmentActivity, ComposeActivity::class.java)
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_TEXT, getTextToSend())
            shareIntent.type = getMimeType()
            fragmentActivity?.startActivity(shareIntent)

            dismiss()
        }

        copyText.setOnClickListener {
            val clipboard = fragmentActivity?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText("messenger", getTextToSend())
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(activity, R.string.message_copied_to_clipboard,
                    Toast.LENGTH_SHORT).show()

            dismiss()
        }

        return contentView
    }

    private fun getTextToSend(): String {
        if (messages == null || messages!!.isEmpty()) {
            return ""
        } else if (messages!!.size == 1) {
            return MessageMultiSelectDelegate.getMessageContent(messages!![0])!!
        }

        return messages!!.filter { it != null }
                .map {
                    val from = if (it!!.type == Message.TYPE_RECEIVED) {
                        // we split it so that we only get the first name,
                        // if there is more than one

                        if (it.from != null) {
                            // it is most likely a group message.
                            it.from
                        } else {
                            conversation?.title ?: "Contact"
                        }
                    } else {
                        getString(R.string.you)
                    }

                    val messageText = when {
                        MimeType.isAudio(it.mimeType!!) -> "<i>" + getString(xyz.klinker.messenger.shared.R.string.audio_message) + "</i>"
                        MimeType.isVideo(it.mimeType!!) -> "<i>" + getString(xyz.klinker.messenger.shared.R.string.video_message) + "</i>"
                        MimeType.isVcard(it.mimeType!!) -> "<i>" + getString(xyz.klinker.messenger.shared.R.string.contact_card) + "</i>"
                        MimeType.isStaticImage(it.mimeType) -> "<i>" + getString(xyz.klinker.messenger.shared.R.string.picture_message) + "</i>"
                        it.mimeType == MimeType.IMAGE_GIF -> "<i>" + getString(xyz.klinker.messenger.shared.R.string.gif_message) + "</i>"
                        MimeType.isExpandedMedia(it.mimeType) -> "<i>" + getString(xyz.klinker.messenger.shared.R.string.media) + "</i>"
                        else -> it.data
                    }

                    "$from: $messageText"
                }.joinToString("\n")
    }

    private fun getMimeType(): String? {
        return if (messages != null && messages!!.size == 1) {
            messages!![0]?.mimeType
        } else {
            MimeType.TEXT_PLAIN
        }
    }

    fun setMessages(messages: List<Message?>, conversation: Conversation?) {
        this.messages = messages
        this.conversation = conversation
    }
}
