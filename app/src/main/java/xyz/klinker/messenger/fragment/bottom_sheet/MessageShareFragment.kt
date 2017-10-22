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
import android.support.v4.app.FragmentActivity

class MessageShareFragment : TabletOptimizedBottomSheetDialogFragment() {

    private val fragmentActivity: FragmentActivity? by lazy { activity }

    private var message: Message? = null

    override fun createLayout(inflater: LayoutInflater): View {
        val contentView = View.inflate(context, R.layout.bottom_sheet_share, null)

        if (fragmentActivity == null) {
            return contentView
        }

        val shareExternal = contentView.findViewById<View>(R.id.share_external)
        val forwardToContact = contentView.findViewById<View>(R.id.forward_to_contact)
        val copyText = contentView.findViewById<View>(R.id.copy_text)

        shareExternal.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_TEXT, MessageMultiSelectDelegate.getMessageContent(message))
            shareIntent.type = message!!.mimeType
            fragmentActivity?.startActivity(Intent.createChooser(shareIntent,
                    activity.resources.getText(R.string.share_content)))

            dismiss()
        }

        forwardToContact.setOnClickListener {
            val shareIntent = Intent(fragmentActivity, ComposeActivity::class.java)
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_TEXT, MessageMultiSelectDelegate.getMessageContent(message))
            shareIntent.type = message!!.mimeType
            fragmentActivity?.startActivity(shareIntent)

            dismiss()
        }

        copyText.setOnClickListener {
            val clipboard = fragmentActivity?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText("messenger",
                    MessageMultiSelectDelegate.getMessageContent(message))
            clipboard?.primaryClip = clip
            Toast.makeText(activity, R.string.message_copied_to_clipboard,
                    Toast.LENGTH_SHORT).show()

            dismiss()
        }

        return contentView
    }

    fun setMessage(message: Message?) {
        if (null == message) {
            this.message = Message()
            this.message!!.data = ""
        } else {
            this.message = message
        }
    }
}
