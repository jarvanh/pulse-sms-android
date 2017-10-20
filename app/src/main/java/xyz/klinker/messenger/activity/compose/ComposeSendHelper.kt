package xyz.klinker.messenger.activity.compose

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Handler
import android.support.design.widget.FloatingActionButton
import android.view.View
import android.view.inputmethod.InputMethodManager
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message

class ComposeSendHelper(private val activity: ComposeActivity) {

    internal val fab: FloatingActionButton by lazy { activity.findViewById<View>(R.id.fab) as FloatingActionButton }

    fun setupViews() {
        fab.backgroundTintList = ColorStateList.valueOf(Settings.mainColorSet.colorAccent)
        fab.setOnClickListener {
            dismissKeyboard()

            Handler().postDelayed({
                if (activity.contactsProvider.hasContacts()) {
                    showConversation()
                }
            }, 100)
        }
    }

    internal fun resetViews(data: String, mimeType: String, isvCard: Boolean = false) {
        fab.setOnClickListener {
            if (activity.contactsProvider.getRecipients().isNotEmpty() && isvCard) {
                activity.vCardSender.send(mimeType, data)
            } else if (activity.contactsProvider.hasContacts()) {
                activity.shareHandler.apply(mimeType, data)
            }
        }
    }

    private fun dismissKeyboard() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(fab.windowToken, 0)
    }

    private fun showConversation() {
        val phoneNumbers = activity.contactsProvider.getPhoneNumberFromContactEntry()
        showConversation(phoneNumbers)
    }

    internal fun showConversation(phoneNumbers: String) {
        var conversationId = DataSource.findConversationId(activity, phoneNumbers)

        // we only want to match on phone number, not by name. This was probably silly?
//        if (conversationId == null && activity.contactsProvider.getRecipients().size == 1) {
//            conversationId = DataSource.findConversationIdByTitle(activity,
//                    activity.contactsProvider.getRecipients()[0].entry.displayName)
//        }

        if (conversationId == null) {
            val message = Message()
            message.type = Message.TYPE_INFO
            message.data = activity.getString(R.string.no_messages_with_contact)
            message.timestamp = System.currentTimeMillis()
            message.mimeType = MimeType.TEXT_PLAIN
            message.read = true
            message.seen = true
            message.sentDeviceId = -1

            conversationId = DataSource.insertMessage(message, phoneNumbers, activity)
        } else {
            DataSource.unarchiveConversation(activity, conversationId)
        }

        val open = Intent(activity, MessengerActivity::class.java)
        open.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, conversationId)
        open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        if (activity.contactsProvider.getRecipients().size == 1) {
            val name = activity.contactsProvider.getRecipients()[0].entry.displayName
            open.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_NAME, name)
        }

        activity.startActivity(open)
        activity.finish()
    }

    internal fun showConversation(conversationId: Long) {
        val open = Intent(activity, MessengerActivity::class.java)
        open.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, conversationId)
        open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        val conversation = DataSource.getConversation(activity, conversationId)
        if (conversation != null && conversation.archive) {
            DataSource.unarchiveConversation(activity, conversationId)
        }

        activity.startActivity(open)
        activity.finish()
    }
}