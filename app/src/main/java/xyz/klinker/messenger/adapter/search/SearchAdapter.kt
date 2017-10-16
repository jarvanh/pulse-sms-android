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

package xyz.klinker.messenger.adapter.search

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.klinker.android.link_builder.Link
import com.klinker.android.link_builder.LinkBuilder
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.listener.SearchListener
import java.util.regex.Pattern

@SuppressLint("Range")
class SearchAdapter(search: String?, private var conversations: MutableList<Conversation>?, private var messages: MutableList<Message>?,
                    listener: SearchListener) : SectionedRecyclerViewAdapter<RecyclerView.ViewHolder>() {

    private var search = search ?: ""
    private val headerBinder = SearchListHeaderBinder(this)
    private val itemBinder = SearchListItemBinder(listener)

    override fun getSectionCount() = 2
    override fun getItemCount(section: Int) = if (section == SearchListHeaderBinder.SECTION_CONVERSATIONS)
            conversations?.size ?: 0 else messages?.size ?: 0

    override fun getHeaderViewType(section: Int) = SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER
    override fun getItemViewType(section: Int, relativePosition: Int, absolutePosition: Int) = if (section == 0)
        VIEW_TYPE_CONVERSATION else messages!![relativePosition].type

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder?, section: Int) {
        val h = holder as ConversationViewHolder
        headerBinder.bind(h, section)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, section: Int,
                                  relativePosition: Int, absolutePosition: Int) {
        val pattern = try {
            Pattern.compile(search, Pattern.CASE_INSENSITIVE)
        } catch (e: Exception) {
            Pattern.compile("")
        }

        val highlight = Link(pattern)
                .setTextColor(ColorSet.DEFAULT(holder.itemView.context).colorAccent)
                .setHighlightAlpha(0.4f)
                .setUnderlined(false)
                .setBold(true)

        if (holder is ConversationViewHolder) {
            val conversation = conversations!![relativePosition]

            holder.name!!.text = conversation.title
            holder.summary!!.text = conversation.snippet

            LinkBuilder.on(holder.name!!)
                    .addLink(highlight)
                    .build()

            itemBinder.bindConversation(holder, conversation)

        } else if (holder is MessageViewHolder) {
            val message = messages!![relativePosition]

            holder.messageId = message.id
            holder.message!!.text = message.data

            LinkBuilder.on(holder.message!!)
                    .addLink(highlight)
                    .build()

            itemBinder.bindMessage(holder, message)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder: RecyclerView.ViewHolder

        when (viewType) {
            SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.conversation_list_header, parent, false)
                holder = ConversationViewHolder(view, null, null)
            }
            VIEW_TYPE_CONVERSATION -> {
                val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.conversation_list_item, parent, false)
                holder = ConversationViewHolder(view, null, null)
            }
            else -> {
                val layoutId: Int
                val color: Int

                val useRounderBubbles = Settings.rounderBubbles
                if (viewType == Message.TYPE_RECEIVED) {
                    layoutId = if (useRounderBubbles) R.layout.message_received_round else R.layout.message_received
                    color = ColorSet.DEFAULT(parent.context).color
                } else {
                    color = -1

                    layoutId = if (viewType == Message.TYPE_SENDING) {
                        if (useRounderBubbles) R.layout.message_sending_round else R.layout.message_sending
                    } else if (viewType == Message.TYPE_ERROR) {
                        if (useRounderBubbles) R.layout.message_error_round else R.layout.message_error
                    } else if (viewType == Message.TYPE_DELIVERED) {
                        if (useRounderBubbles) R.layout.message_delivered_round else R.layout.message_delivered
                    } else if (viewType == Message.TYPE_INFO) {
                        R.layout.message_info
                    } else {
                        if (useRounderBubbles) R.layout.message_sent_round else R.layout.message_sent
                    }
                }

                val view = LayoutInflater.from(parent.context)
                        .inflate(layoutId, parent, false)

                holder = MessageViewHolder(null, view, color, viewType, null)
            }
        }

        return holder
    }

    fun updateCursors(search: String?, conversations: MutableList<Conversation>, messages: MutableList<Message>) {
        this.conversations?.clear()
        this.messages?.clear()

        this.search = search ?: ""
        this.conversations = conversations
        this.messages = messages

        notifyDataSetChanged()
    }

    companion object {
        private val VIEW_TYPE_CONVERSATION = -3
    }
}
