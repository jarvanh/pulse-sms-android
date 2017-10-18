package xyz.klinker.messenger.fragment.message

import android.os.Handler
import android.support.v7.widget.Toolbar
import android.view.View
import xyz.klinker.messenger.R
import xyz.klinker.messenger.fragment.MessageListFragment
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.pojo.ConversationUpdateInfo
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils

class ConversationInformationUpdater(private val fragment: MessageListFragment) {

    private val activity
        get() = fragment.activity
    private val argManager
        get() = fragment.argManager

    private val handler = Handler()
    private val toolbar: Toolbar by lazy { fragment.rootView!!.findViewById<View>(R.id.toolbar) as Toolbar }

    fun update() {
        val number = argManager.phoneNumbers
        val name = ContactUtils.findContactNames(number, activity)
        var photoUri = ContactUtils.findImageUri(number, activity)
        if (photoUri != null && !photoUri.isEmpty()) {
            photoUri += "/photo"
        }

        if (name != argManager.title && !PhoneNumberUtils.checkEquality(name, number)) {
            DataSource.updateConversationTitle(activity, argManager.conversationId, name)

            val fragment = activity.supportFragmentManager.findFragmentById(R.id.conversation_list_container) as ConversationListFragment
            fragment.setNewConversationTitle(name)

            handler.post { toolbar.title = name }
        }

        val originalImage = argManager.imageUri
        if (photoUri != null && (originalImage == null || photoUri != originalImage || originalImage.isEmpty())) {
            DataSource.updateConversationImage(activity, argManager.conversationId, photoUri)
        }
    }

    fun setConversationUpdateInfo(newMessage: String) {
        val fragment = activity.supportFragmentManager.findFragmentById(R.id.conversation_list_container)
        if (fragment != null && fragment is ConversationListFragment) {
            fragment.setConversationUpdateInfo(ConversationUpdateInfo(argManager.conversationId, newMessage, true))
        }
    }
}