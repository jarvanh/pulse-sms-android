package xyz.klinker.messenger.fragment.message

import android.os.Handler
import androidx.appcompat.widget.Toolbar
import android.view.View
import androidx.fragment.app.FragmentActivity
import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.pojo.ConversationUpdateInfo
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.PhoneNumberUtils

class ConversationInformationUpdater(private val fragment: MessageListFragment) {

    private val activity: FragmentActivity? by lazy { fragment.activity }
    private val argManager
        get() = fragment.argManager

    private val handler = Handler()
    private val toolbar: Toolbar by lazy { fragment.rootView!!.findViewById<View>(R.id.toolbar) as Toolbar }

    fun update() {
        if (activity == null) {
            return
        }

        val number = argManager.phoneNumbers
        val name = ContactUtils.findContactNames(number, activity)
        var photoUri = ContactUtils.findImageUri(number, activity)
        if (photoUri != null && !photoUri.isEmpty()) {
            photoUri += "/photo"
        }

        if (name != argManager.title && !PhoneNumberUtils.checkEquality(name, number)) {
            if (!Account.exists() || Account.primary) {
                DataSource.updateConversationTitle(activity!!, argManager.conversationId, name)

                val fragment = activity?.supportFragmentManager?.findFragmentById(R.id.conversation_list_container) as ConversationListFragment?
                fragment?.setNewConversationTitle(name)

                handler.post { toolbar.title = name }
            }
        }

        val originalImage = argManager.imageUri
        if (photoUri != null && (originalImage == null || photoUri != originalImage || originalImage.isEmpty()) && activity != null) {
            DataSource.updateConversationImage(activity!!, argManager.conversationId, photoUri)
        }
    }

    fun setConversationUpdateInfo(newMessage: String) {
        val fragment = activity?.supportFragmentManager?.findFragmentById(R.id.conversation_list_container)
        if (fragment != null && fragment is ConversationListFragment) {
            fragment.setConversationUpdateInfo(ConversationUpdateInfo(argManager.conversationId, newMessage, true))
        }
    }
}