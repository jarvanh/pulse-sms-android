package xyz.klinker.messenger.fragment

import android.support.design.widget.NavigationView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View

import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeTouchHelper
import xyz.klinker.messenger.utils.swipe_to_dismiss.UnarchiveSwipeSimpleCallback

class ArchivedConversationListFragment : ConversationListFragment() {

    // always consume the back event and send us to the conversation list
    override fun onBackPressed(): Boolean {
        if (!super.onBackPressed()) {
            val navView = activity.findViewById<View>(R.id.navigation_view) as NavigationView
            navView.menu.getItem(1).isChecked = true

            activity.title = getString(R.string.app_title)
            (activity as MessengerActivity).displayConversations()
        }

        return true
    }

    override fun onConversationContracted(viewHolder: ConversationViewHolder) {
        super.onConversationContracted(viewHolder)

        val navView = activity.findViewById<View>(R.id.navigation_view) as NavigationView
        navView.menu.getItem(2).isChecked = true
    }
}
