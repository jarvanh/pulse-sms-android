package xyz.klinker.messenger.fragment

import android.app.Activity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation

class FolderConversationListFragment : ConversationListFragment() {

    private var folderId: Long = 0L
    private var drawerIndex: Int = 5

    override fun noConversationsText() = getString(R.string.no_folder_messages_description)

    override fun onCreateView(inflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?): View? {
        val view = super.onCreateView(inflater, viewGroup, bundle)

        folderId = arguments?.getLong(ARG_FOLDER_ID) ?: 0L
        drawerIndex = arguments?.getInt(ARG_DRAWER_INDEX) ?: 5

        return view
    }

    // always consume the back event and send us to the conversation list
    override fun onBackPressed(): Boolean {
        if (!super.onBackPressed()) {
            val navView = activity?.findViewById<View>(R.id.navigation_view) as NavigationView?
            navView?.menu?.getItem(1)?.isChecked = true

            activity?.title = getString(R.string.app_title)
            (activity as MessengerActivity).displayConversations()
        }

        return true
    }

    override fun onConversationContracted(viewHolder: ConversationViewHolder) {
        super.onConversationContracted(viewHolder)

        val navView = activity?.findViewById<View>(R.id.navigation_view) as NavigationView?
        navView?.menu?.getItem(drawerIndex)?.isChecked = true
    }

    fun queryConversations(activity: Activity): List<Conversation> {
        return DataSource.getFolderConversationsAsList(activity, folderId)
    }

    companion object {
        private const val ARG_FOLDER_ID = "arg_folder_id"
        private const val ARG_DRAWER_INDEX = "arg_drawer_index"

        fun getInstance(folderId: Long, drawerIndex: Int): FolderConversationListFragment {
            val args = Bundle()
            args.putLong(ARG_FOLDER_ID, folderId)
            args.putInt(ARG_DRAWER_INDEX, drawerIndex)

            val fragment = FolderConversationListFragment()
            fragment.arguments = args

            return fragment
        }
    }
}
