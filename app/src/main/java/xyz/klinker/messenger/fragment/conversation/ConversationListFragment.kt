/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.fragment.conversation

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.adapter.conversation.ConversationListAdapter
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.SectionType
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.pojo.ConversationUpdateInfo
import xyz.klinker.messenger.shared.shared_interfaces.IConversationListFragment
import xyz.klinker.messenger.shared.util.SnackbarAnimationFix
import xyz.klinker.messenger.shared.util.TimeUtils
import xyz.klinker.messenger.shared.util.listener.BackPressedListener
import xyz.klinker.messenger.utils.listener.ConversationExpandedListener
import xyz.klinker.messenger.utils.multi_select.ConversationsMultiSelectDelegate
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeToDeleteListener
import java.util.*

/**
 * Fragment for displaying the conversation list or an empty screen if there are currently no
 * open conversations.
 */
open class ConversationListFragment : Fragment(), SwipeToDeleteListener, ConversationExpandedListener, BackPressedListener, IConversationListFragment {

    private val fragmentActivity: FragmentActivity? by lazy { activity }

    val messageListManager: MessageListManager by lazy { MessageListManager(this) }
    val swipeHelper: ConversationSwipeHelper by lazy { ConversationSwipeHelper(this) }
    val updateHelper: ConversationUpdateHelper by lazy { ConversationUpdateHelper(this) }
    val recyclerManager: ConversationRecyclerViewManager by lazy { ConversationRecyclerViewManager(this) }
    val multiSelector: ConversationsMultiSelectDelegate by lazy { ConversationsMultiSelectDelegate(this) }

    var rootView: View? = null
    var lastRefreshTime: Long = 0

    override fun onCreateView(inflater: LayoutInflater, viewGroup: ViewGroup?, bundle: Bundle?): View? {
        rootView = inflater!!.inflate(R.layout.fragment_conversation_list, viewGroup, false)

        recyclerManager.setupViews()
        recyclerManager.loadConversations()

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateHelper.createReceiver()
    }

    override fun onStart() {
        super.onStart()

        if (TimeUtils.now - lastRefreshTime > 1000 * 60 * 60 && !isExpanded) {
            recyclerManager.loadConversations()
        }

        if (messageListManager.messageListFragment != null && !messageListManager.messageListFragment!!.isAdded) {
            val main = Intent(fragmentActivity, MessengerActivity::class.java)
            main.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID,
                    messageListManager.messageListFragment!!.conversationId)

            fragmentActivity?.overridePendingTransition(0, 0)
            fragmentActivity?.finish()

            fragmentActivity?.overridePendingTransition(0, 0)
            fragmentActivity?.startActivity(main)
        }
    }

    override fun onBackPressed(): Boolean {
        return if (messageListManager.messageListFragment != null && messageListManager.messageListFragment!!.onBackPressed()) true
        else if (messageListManager.expandedConversation != null) {
            val conversation = messageListManager.expandedConversation!!.itemView
            conversation.isSoundEffectsEnabled = false
            conversation.performClick()
            conversation.isSoundEffectsEnabled = true
            true
        } else false
    }

    override fun onDestroyView() {
        super.onDestroyView()

        updateHelper.destroyReceiver()
        multiSelector.clearActionMode()
    }

    override fun onShowMarkAsRead(sectionText: String) {
        Toast.makeText(fragmentActivity, getString(R.string.mark_section_as_read, sectionText.toLowerCase(Locale.US)), Toast.LENGTH_LONG).show()
    }

    override fun onMarkSectionAsRead(sectionText: String, sectionType: Int) {
        val snackbar = Snackbar.make(recyclerView, getString(R.string.marking_section_as_read, sectionText.toLowerCase(Locale.US)), Snackbar.LENGTH_LONG)
        SnackbarAnimationFix.apply(snackbar)
        snackbar.show()

        val allConversations = adapter?.conversations
        val markAsRead = ArrayList<Conversation>()

        if (allConversations == null) {
            return
        }

        val handler = Handler()
        Thread {
            for (conversation in allConversations) {
                var shouldRead = false
                when (sectionType) {
                    SectionType.PINNED -> shouldRead = conversation.pinned
                    SectionType.TODAY -> shouldRead = TimeUtils.isToday(conversation.timestamp)
                    SectionType.YESTERDAY -> shouldRead = TimeUtils.isYesterday(conversation.timestamp)
                    SectionType.LAST_WEEK -> shouldRead = TimeUtils.isLastWeek(conversation.timestamp)
                    SectionType.LAST_MONTH -> shouldRead = TimeUtils.isLastMonth(conversation.timestamp)
                }

                if (shouldRead) {
                    markAsRead.add(conversation)
                }
            }

            if (fragmentActivity != null) {
                DataSource.readConversations(fragmentActivity!!, markAsRead)
            }
            handler.post { recyclerManager.loadConversations() }
        }.start()
    }

    override fun onSwipeToDelete(conversation: Conversation) { swipeHelper.onSwipeToDelete(conversation) }
    override fun onSwipeToArchive(conversation: Conversation) { swipeHelper.onSwipeToArchive(conversation) }
    override fun onConversationExpanded(viewHolder: ConversationViewHolder): Boolean { return messageListManager.onConversationExpanded(viewHolder) }
    override fun onConversationContracted(viewHolder: ConversationViewHolder) { messageListManager.onConversationContracted() }
    override fun checkEmptyViewDisplay() { recyclerManager.checkEmptyViewDisplay() }

    fun notifyOfSentMessage(m: Message) { updateHelper.notifyOfSentMessage(m) }
    fun setConversationUpdateInfo(info: ConversationUpdateInfo) { updateHelper.updateInfo = info }
    fun setNewConversationTitle(title: String) { updateHelper.newConversationTitle = title }

    override val isFragmentAdded: Boolean
        get() = !isDetached

    override val adapter: ConversationListAdapter?
        get() = recyclerManager.adapter

    override val expandedId: Long
        get() = if (isExpanded)  messageListManager.expandedConversation!!.conversation!!.id else 0

    val isExpanded: Boolean
        get() = messageListManager.expandedConversation != null

    val expandedItem: ConversationViewHolder?
        get() = messageListManager.expandedConversation

    val recyclerView: RecyclerView
        get() = recyclerManager.recyclerView

    companion object {
        fun newInstance(conversationToOpenId: Long = -1, messageToOpenId: Long = -1): ConversationListFragment {
            val fragment = ConversationListFragment()
            val bundle = Bundle()

            if (conversationToOpenId != -1L) {
                bundle.putLong(MessageListManager.ARG_CONVERSATION_TO_OPEN_ID, conversationToOpenId)
            }

            if (messageToOpenId != -1L) {
                bundle.putLong(MessageListManager.ARG_MESSAGE_TO_OPEN_ID, messageToOpenId)
            }

            fragment.arguments = bundle
            return fragment
        }
    }

}
