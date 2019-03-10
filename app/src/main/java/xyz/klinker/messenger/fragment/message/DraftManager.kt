package xyz.klinker.messenger.fragment.message

import android.app.Notification
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import xyz.klinker.messenger.R
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.model.Draft
import xyz.klinker.messenger.shared.data.pojo.ConversationUpdateInfo
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver

class DraftManager(private val fragment: MessageListFragment) {

    private val activity: FragmentActivity? by lazy { fragment.activity }
    private val attachManager
        get() = fragment.attachManager
    private val argManager
        get() = fragment.argManager

    private val messageEntry: EditText by lazy { fragment.rootView!!.findViewById<View>(R.id.message_entry) as EditText }
    private val editImage: View by lazy { fragment.rootView!!.findViewById<View>(R.id.edit_image) }
    private val sendProgress: View by lazy { fragment.rootView!!.findViewById<View>(R.id.send_progress) }
    private val selectedImageCount: View by lazy { fragment.rootView!!.findViewById<View>(R.id.selected_images) }
    private val selectedImageCountText: TextView by lazy { fragment.rootView!!.findViewById<View>(R.id.selected_images_text) as TextView }

    var pullDrafts = true
    private var drafts = emptyList<Draft>()

    fun applyDrafts() { if (pullDrafts) setDrafts(drafts) else pullDrafts = true }
    fun loadDrafts() {
        if (activity != null) drafts = DataSource.getDrafts(activity!!, argManager.conversationId)
        if (drafts.isNotEmpty()) {
            val activity = this.activity!!
            Thread {
                val updatedSnippet = DataSource.deleteDrafts(activity, argManager.conversationId)
                if (updatedSnippet != null) {
                    activity.runOnUiThread {
                        val fragment = activity.supportFragmentManager.findFragmentById(R.id.conversation_list_container)
                        if (fragment != null && fragment is ConversationListFragment) {
                            fragment.setConversationUpdateInfo(ConversationUpdateInfo(argManager.conversationId, updatedSnippet, true))
                        }
                    }
                }
            }.start()
        }
    }

    fun createDrafts() {
        if (sendProgress.visibility != View.VISIBLE && messageEntry.text != null && messageEntry.text.isNotEmpty()) {
            if (drafts.isNotEmpty() && activity != null) {
                DataSource.deleteDrafts(activity!!, argManager.conversationId)
            }

            DataSource.insertDraft(activity, argManager.conversationId,
                    messageEntry.text.toString(), MimeType.TEXT_PLAIN)
        }

        attachManager.writeDraftOfAttachment()
    }

    private fun setDrafts(drafts: List<Draft>) {
        val notificationDraft = fragment.argManager.notificationInputDraft
        if (!notificationDraft.isNullOrBlank()) {
            messageEntry.setText(notificationDraft)

            val extra: String? = null
            fragment.activity?.intent?.putExtra(Notification.EXTRA_REMOTE_INPUT_DRAFT, extra)
        } else {
            for (draft in drafts) {
                when {
                    draft.mimeType == MimeType.TEXT_PLAIN -> {
                        messageEntry.setText(draft.data)
                        messageEntry.setSelection(messageEntry.text.length)
                    }
                    MimeType.isStaticImage(draft.mimeType) -> {
                        val uri = Uri.parse(draft.data)
                        attachManager.attachImage(uri)
                        attachManager.selectedImageUris.add(uri.toString())

                        if (attachManager.selectedImageUris.size > 1) {
                            selectedImageCount.visibility = View.VISIBLE
                            selectedImageCountText.text = attachManager.selectedImageUris.size.toString()
                            editImage.visibility = View.GONE
                        }
                    }
                    draft.mimeType == MimeType.IMAGE_GIF -> {
                        attachManager.attachImage(Uri.parse(draft.data))
                        attachManager.attachedMimeType = draft.mimeType
                        editImage.visibility = View.GONE
                    }
                    draft.mimeType!!.contains("audio/") -> {
                        attachManager.attachAudio(Uri.parse(draft.data))
                        attachManager.attachedMimeType = draft.mimeType
                        editImage.visibility = View.GONE
                    }
                    draft.mimeType!!.contains("video/") -> {
                        attachManager.attachImage(Uri.parse(draft.data))
                        attachManager.attachedMimeType = draft.mimeType
                        editImage.visibility = View.GONE
                    }
                    draft.mimeType == MimeType.TEXT_VCARD -> {
                        attachManager.attachContact(Uri.parse(draft.data))
                        attachManager.attachedMimeType = draft.mimeType
                        editImage.visibility = View.GONE
                    }
                }
            }
        }
    }
}