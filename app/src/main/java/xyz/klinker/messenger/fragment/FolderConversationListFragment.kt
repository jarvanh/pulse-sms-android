package xyz.klinker.messenger.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.navigation.NavigationView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Folder

class FolderConversationListFragment : ConversationListFragment() {

    private val navView: NavigationView? by lazy { activity?.findViewById<NavigationView>(R.id.navigation_view) }

    private var folderId: Long = 0L

    override fun noConversationsText() = getString(R.string.no_folder_messages_description)

    override fun onCreateView(inflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?): View? {
        val view = super.onCreateView(inflater, viewGroup, bundle)

        folderId = arguments?.getLong(ARG_FOLDER_ID) ?: 0L

        return view
    }

    // always consume the back event and send us to the conversation list
    override fun onBackPressed(): Boolean {
        if (!super.onBackPressed()) {
            navView?.menu?.findItem(R.id.drawer_conversation)?.isChecked = true

            activity?.title = getString(R.string.app_title)
            (activity as MessengerActivity).displayConversations()
        }

        return true
    }

    override fun onConversationContracted(viewHolder: ConversationViewHolder) {
        super.onConversationContracted(viewHolder)
        navView?.postDelayed({
            navView?.menu?.findItem(folderId.toInt())?.isChecked = true
        }, 300)
    }

    fun queryConversations(activity: Activity): List<Conversation> {
        return DataSource.getFolderConversationsAsList(activity, folderId)
    }

    companion object {
        private const val ARG_FOLDER_ID = "arg_folder_id"

        fun getInstance(folder: Folder): FolderConversationListFragment {
            val args = Bundle()
            args.putLong(ARG_FOLDER_ID, folder.id)

            val fragment = FolderConversationListFragment()
            fragment.arguments = args

            return fragment
        }
    }
}
