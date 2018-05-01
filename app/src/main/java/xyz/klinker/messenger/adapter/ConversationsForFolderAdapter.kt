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

package xyz.klinker.messenger.adapter

import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.util.listener.ContactClickedListener

/**
 * Adds a checkbox to the base contact adapter so that you can select multiple items.
 */
class ConversationsForFolderAdapter(conversations: List<Conversation>, listener: ContactClickedListener,
                                    private val selectedIds: List<Long>, private val thisFolderId: Long) : ContactAdapter(conversations, listener) {

    override val layoutId: Int
        get() = R.layout.invite_list_item

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val conversation = conversations[position]
        holder.checkBox?.isChecked = selectedIds.contains(conversation.id)

        if (conversation.privateNotifications) {
            holder.summary?.text = holder.itemView.context.getString(R.string.private_conversations_cannot_be_added_to_folders)
            holder.itemView.isEnabled = false
            holder.itemView.isFocusable = false
            holder.itemView.alpha = .3f
        } else if (conversation.folderId != -1L && conversation.folderId != thisFolderId) {
            holder.summary?.text = holder.itemView.context.getString(R.string.conversation_already_in_a_folder)
            holder.itemView.isEnabled = false
            holder.itemView.isFocusable = false
            holder.itemView.alpha = .3f
        } else {
            holder.itemView.isEnabled = true
            holder.itemView.isFocusable = true
            holder.itemView.alpha = 1.0f
        }
    }
}
