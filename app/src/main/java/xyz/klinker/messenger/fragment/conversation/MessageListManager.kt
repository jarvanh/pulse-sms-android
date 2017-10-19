package xyz.klinker.messenger.fragment.conversation

import android.os.Handler
import android.util.Log
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.fragment.MessageListFragment
import xyz.klinker.messenger.fragment.message.MessageInstanceManager
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.AnimationUtils
import xyz.klinker.messenger.shared.util.ColorUtils

class MessageListManager(private val fragment: ConversationListFragment) {

    companion object {
        internal val ARG_CONVERSATION_TO_OPEN_ID = "conversation_to_open"
        internal val ARG_MESSAGE_TO_OPEN_ID = "message_to_open"
    }

    private val clickHandler: Handler by lazy { Handler() }

    var expandedConversation: ConversationViewHolder? = null
    var messageListFragment: MessageListFragment? = null

    fun onConversationExpanded(viewHolder: ConversationViewHolder): Boolean {
        fragment.updateHelper.updateInfo = null

        if (expandedConversation != null) {
            return false
        }

        fragment.swipeHelper.dismissSnackbars(fragment.activity)

        expandedConversation = viewHolder
        AnimationUtils.expandActivityForConversation(fragment.activity)

        if (fragment.arguments != null && fragment.arguments.containsKey(ARG_MESSAGE_TO_OPEN_ID)) {
            messageListFragment = MessageInstanceManager.newInstance(viewHolder.conversation!!,
                    fragment.arguments.getLong(ARG_MESSAGE_TO_OPEN_ID))
            fragment.arguments.remove(ARG_MESSAGE_TO_OPEN_ID)
        } else {
            messageListFragment = MessageInstanceManager.newInstance(viewHolder.conversation!!)
        }

        try {
            fragment.activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.message_list_container, messageListFragment)
                    .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }


        if (!Settings.useGlobalThemeColor) {
            ActivityUtils.setTaskDescription(fragment.activity,
                    viewHolder.conversation!!.title!!, viewHolder.conversation!!.colors.color)
        }

        fragment.recyclerManager.canScroll(false)

        fragment.activity?.intent?.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, -1L)
        fragment.arguments?.putLong(ARG_CONVERSATION_TO_OPEN_ID, -1L)

        return true
    }

    fun onConversationContracted() {
        expandedConversation = null
        AnimationUtils.contractActivityFromConversation(fragment.activity)

        if (messageListFragment == null) {
            return
        }

        val contractedId = messageListFragment!!.conversationId
        messageListFragment?.view?.animate()?.alpha(0f)?.setDuration(100)?.start()

        try {
            Handler().postDelayed({
                if (fragment.activity != null) {
                    try {
                        fragment.activity.supportFragmentManager.beginTransaction()
                                .remove(messageListFragment)
                                .commit()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                messageListFragment = null
            }, AnimationUtils.EXPAND_CONVERSATION_DURATION.toLong())
        } catch (e: Exception) {
        }

        val color = Settings.mainColorSet
        ColorUtils.adjustStatusBarColor(color.colorDark, fragment.activity)
        ColorUtils.adjustDrawerColor(color.colorDark, fragment.activity)

        ColorUtils.changeRecyclerOverscrollColors(fragment.recyclerView, color.color)
        ActivityUtils.setTaskDescription(fragment.activity)

        fragment.updateHelper.broadcastUpdateInfo()
        fragment.updateHelper.broadcastTitleChange(contractedId)

        fragment.recyclerManager.canScroll(true)
    }

    fun tryOpeningFromArguments() {
        if (fragment.arguments != null) {
            val conversationToOpen = fragment.arguments.getLong(ARG_CONVERSATION_TO_OPEN_ID, -1L)
            fragment.arguments.putLong(ARG_CONVERSATION_TO_OPEN_ID, -1L)

            if (conversationToOpen != -1L) {
                clickConversationWithId(conversationToOpen)
            }
        } else {
            Log.v("Conversation List", "no conversations to open")
        }
    }

    private fun clickConversationWithId(id: Long) {
        val conversationPosition = fragment.adapter?.findPositionForConversationId(id) ?: return
        if (conversationPosition != -1) {
            fragment.recyclerManager.scrollToPosition(conversationPosition)
            clickConversationAtPosition(conversationPosition)
        }
    }

    private fun clickConversationAtPosition(position: Int) {
        clickHandler.removeCallbacksAndMessages(null)
        clickHandler.postDelayed({
            try {
                val itemView = fragment.recyclerManager.getViewAtPosition(position)
                itemView.isSoundEffectsEnabled = false
                itemView.performClick()
                itemView.isSoundEffectsEnabled = true
            } catch (e: Exception) {
                // not yet ready to click
                clickConversationAtPosition(position)
            }
        }, 100)
    }

}