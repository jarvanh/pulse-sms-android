package xyz.klinker.messenger.fragment

import android.support.design.widget.NavigationView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View

import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.adapter.ConversationListAdapter
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeTouchHelper
import xyz.klinker.messenger.utils.swipe_to_dismiss.UnarchiveSwipeSimpleCallback

class ArchivedConversationListFragment : ConversationListFragment() {

    // only grab the archived messages
    override fun getCursor(source: DataSource): List<Conversation> {
        return source.getArchivedConversationsAsList(activity)
    }

    // create swipe helper that has the unarchive icon instead of the archive one
    override fun getSwipeTouchHelper(adapter: ConversationListAdapter): ItemTouchHelper {
        return SwipeTouchHelper(UnarchiveSwipeSimpleCallback(adapter))
    }

    // change the text to "1 conversation moved to the inbox
    override fun getArchiveSnackbarText(): String {
        return resources.getQuantityString(R.plurals.conversations_unarchived,
                pendingArchive.size, pendingArchive.size)
    }

    // unarchive instead of archive when the snackbar is dismissed
    override fun performArchiveOperation(dataSource: DataSource, conversation: Conversation) {
        dataSource.unarchiveConversation(activity, conversation.id)
    }

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
