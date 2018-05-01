package xyz.klinker.messenger.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.fragment.conversation.ConversationListFragment
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeTouchHelper
import xyz.klinker.messenger.utils.swipe_to_dismiss.UnarchiveSwipeSimpleCallback

class PrivateConversationListFragment : ConversationListFragment() {

    override fun onCreateView(inflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?): View? {
        val view = super.onCreateView(inflater, viewGroup, bundle)

        val fragmentActivity = activity
        if (fragmentActivity != null && Settings.privateConversationsPasscode.isNullOrBlank()) {
            val prefs = Settings.getSharedPrefs(fragmentActivity)
            if (prefs.getBoolean("private_conversation_security_disclainer", true)) {
                AlertDialog.Builder(fragmentActivity)
                        .setMessage(R.string.enable_passcode_disclaimer)
                        .setPositiveButton(android.R.string.ok, { _, _ -> })
                        .setNegativeButton(R.string.menu_feature_settings, { _, _ ->
                            if (fragmentActivity is MessengerActivity) {
                                fragmentActivity.clickNavigationItem(R.id.drawer_feature_settings)
                            }
                        }).show()

                prefs.edit().putBoolean("private_conversation_security_disclainer", false).commit()
            }
        }


        return view
    }

    override fun noConversationsText() = getString(R.string.no_private_messages_description)

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
        navView?.menu?.findItem(R.id.drawer_private)?.isChecked = true
    }
}
