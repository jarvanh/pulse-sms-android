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
import com.klinker.android.link_builder.applyLinks
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder
import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.pojo.BubbleTheme
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

            holder.name?.applyLinks(highlight)
            itemBinder.bindConversation(holder, conversation)
        } else if (holder is MessageViewHolder) {
            val message = messages!![relativePosition]

            holder.messageId = message.id
            holder.message!!.text = message.data

            holder.message?.applyLinks(highlight)
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

                if (viewType == Message.TYPE_RECEIVED) {
                    layoutId = when (Settings.bubbleTheme) {
                        BubbleTheme.ROUNDED -> R.layout.message_round_received
                        BubbleTheme.CIRCLE -> R.layout.message_circle_received
                        BubbleTheme.SQUARE -> R.layout.message_square_received
                    }
                    color = Settings.mainColorSet.color
                } else {
                    color = Integer.MIN_VALUE
                    layoutId = when (Settings.bubbleTheme) {
                        BubbleTheme.ROUNDED -> when (viewType) {
                            Message.TYPE_SENDING -> R.layout.message_round_sending
                            Message.TYPE_ERROR -> R.layout.message_round_error
                            Message.TYPE_DELIVERED -> R.layout.message_round_delivered
                            Message.TYPE_IMAGE_SENDING -> R.layout.message_round_image_sending
                            Message.TYPE_IMAGE_SENT -> R.layout.message_round_image_sent
                            Message.TYPE_IMAGE_RECEIVED -> R.layout.message_round_image_received
                            Message.TYPE_INFO -> R.layout.message_info
                            Message.TYPE_MEDIA -> R.layout.message_media
                            else -> R.layout.message_round_sent
                        }
                        BubbleTheme.CIRCLE -> when (viewType) {
                            Message.TYPE_SENDING -> R.layout.message_circle_sending
                            Message.TYPE_ERROR -> R.layout.message_circle_error
                            Message.TYPE_DELIVERED -> R.layout.message_circle_delivered
                            Message.TYPE_IMAGE_SENDING -> R.layout.message_circle_image_sending
                            Message.TYPE_IMAGE_SENT -> R.layout.message_circle_image_sent
                            Message.TYPE_IMAGE_RECEIVED -> R.layout.message_circle_image_received
                            Message.TYPE_INFO -> R.layout.message_info
                            Message.TYPE_MEDIA -> R.layout.message_media
                            else -> R.layout.message_circle_sent
                        }
                        BubbleTheme.SQUARE -> when (viewType) {
                            Message.TYPE_SENDING -> R.layout.message_square_sending
                            Message.TYPE_ERROR -> R.layout.message_square_error
                            Message.TYPE_DELIVERED -> R.layout.message_square_delivered
                            Message.TYPE_IMAGE_SENDING -> R.layout.message_square_image_sending
                            Message.TYPE_IMAGE_SENT -> R.layout.message_square_image_sent
                            Message.TYPE_IMAGE_RECEIVED -> R.layout.message_square_image_received
                            Message.TYPE_INFO -> R.layout.message_info
                            Message.TYPE_MEDIA -> R.layout.message_media
                            else -> R.layout.message_square_sent
                        }
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
        private const val VIEW_TYPE_CONVERSATION = -3
    }
}
