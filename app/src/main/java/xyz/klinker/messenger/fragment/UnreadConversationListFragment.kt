package xyz.klinker.messenger.fragment

import android.support.design.widget.NavigationView
import android.view.View
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment

class UnreadConversationListFragment : ConversationListFragment() {

    // always consume the back event and send us to the conversation list
    override fun onBackPressed(): Boolean {
        if (!super.onBackPressed()) {
            val navView = activity?.findViewById<View>(R.id.navigation_view) as NavigationView?
            navView?.menu?.findItem(R.id.drawer_conversation)?.isChecked = true

            activity?.title = getString(R.string.app_title)
            (activity as MessengerActivity).displayConversations()
        }

        return true
    }

    override fun onConversationContracted(viewHolder: ConversationViewHolder) {
        super.onConversationContracted(viewHolder)

        val navView = activity?.findViewById<View>(R.id.navigation_view) as NavigationView?
        navView?.menu?.findItem(R.id.drawer_unread)?.isChecked = true
    }
}
