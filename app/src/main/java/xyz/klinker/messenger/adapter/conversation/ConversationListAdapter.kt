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

package xyz.klinker.messenger.adapter.conversation

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.bumptech.glide.Glide
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.data.SectionType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.pojo.ReorderType
import xyz.klinker.messenger.shared.shared_interfaces.IConversationListAdapter
import xyz.klinker.messenger.shared.util.ContactUtils
import xyz.klinker.messenger.shared.util.TimeUtils
import xyz.klinker.messenger.utils.listener.ConversationExpandedListener
import xyz.klinker.messenger.utils.multi_select.ConversationsMultiSelectDelegate
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeToDeleteListener
import java.util.*

/**
 * Adapter for displaying conversation items in a list. The adapter splits items into different
 * sections depending on whether they are pinned and when the last message was received.
 */
class ConversationListAdapter(context: MessengerActivity, initialConversations: List<Conversation>,
                              val multiSelector: ConversationsMultiSelectDelegate?,
                              var swipeToDeleteListener: SwipeToDeleteListener,
                              private val conversationExpandedListener: ConversationExpandedListener)
    : SectionedRecyclerViewAdapter<ConversationViewHolder>(), IConversationListAdapter {

    private val dataProvider: ConversationAdapterDataProvider = ConversationAdapterDataProvider(this, context)
    private val itemBinder: ConversationItemBinder = ConversationItemBinder(context)
    private val headerBinder: ConversationSectionHeaderBinder = ConversationSectionHeaderBinder(this, dataProvider, context)

    override var conversations: MutableList<Conversation>
        get() = dataProvider.conversations
        set(convos) = dataProvider.generateSections(convos)

    override val sectionCounts: MutableList<SectionType>
        get() = dataProvider.sectionCounts

    init {
        this.conversations = initialConversations.toMutableList()
        if (this.multiSelector != null) this.multiSelector.setAdapter(this)

        shouldShowHeadersForEmptySections(showHeaderAboutTextingOnline())
    }

    override fun getSectionCount() = sectionCounts.size
    override fun getItemCount(section: Int) = sectionCounts[section].count

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(if (viewType == SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER)
            R.layout.conversation_list_header else R.layout.conversation_list_item,
                parent, false)

        return ConversationViewHolder(view, conversationExpandedListener, this)
    }

    override fun onViewRecycled(holder: ConversationViewHolder) {
        super.onViewRecycled(holder)
        try {
            Glide.with(holder.itemView.context).clear(holder.image!!)
        } catch (t: Throwable) { }
    }

    override fun onBindHeaderViewHolder(holder: ConversationViewHolder, section: Int) {
        if (sectionCounts[section].type == SectionType.CARD_ABOUT_ONLINE) {
            headerBinder.bindOnlinePromotion(holder)
        } else {
            headerBinder.bind(holder, section)
        }
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, section: Int, relativePosition: Int,
                                  absolutePosition: Int) {
        if (absolutePosition >= dataProvider.conversations.size || absolutePosition < 0) {
            return
        }

        val conversation = dataProvider.conversations[absolutePosition]

        // somehow a null conversation is being inserted in here sometimes after a new
        // conversation is created on the phone and the tablet gets a broadcast for it. Don't know
        // why this happens, but the situation is marked by a blank holder in the conversation list.
        //        if (conversation == null) {
        //            itemBinder.nullItem(holder);
        //            return;
        //        }

        holder.conversation = conversation
        holder.absolutePosition = absolutePosition

        itemBinder.showText(holder, conversation)
        itemBinder.showTextStyle(holder, conversation)
        itemBinder.indicatePinned(holder, conversation)
        itemBinder.showDate(holder, conversation)
        itemBinder.showImageColor(holder, conversation)

        if (ContactUtils.shouldDisplayContactLetter(conversation)) {
            itemBinder.showContactLetter(holder, conversation)
        } else {
            itemBinder.showContactPlaceholderIcon(holder, conversation)
        }

        if (conversation.imageUri != null && conversation.imageUri!!.isNotEmpty()) {
            itemBinder.showImage(holder, conversation)
        }
    }

    override fun getCountForSection(sectionType: Int) = dataProvider.getCountForSection(sectionType)
    override fun findPositionForConversationId(id: Long) = dataProvider.findPositionForConversationId(id)
    override fun removeItem(position: Int, type: ReorderType) = dataProvider.removeItem(position, type)
    fun deleteItem(position: Int) = removeItem(position, ReorderType.DELETE)
    fun archiveItem(position: Int) = removeItem(position, ReorderType.ARCHIVE)
    fun findConversationForPosition(position: Int) = dataProvider.findConversationForPosition(position)
    fun showHeaderAboutTextingOnline() = Build.FINGERPRINT != "robolectric" && !Account.exists() &&
            Settings.showTextOnlineOnConversationList &&
            Math.abs(Settings.installTime - Date().time) > TimeUtils.MINUTE * 0
}
