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
class InviteFriendsAdapter(conversations: List<Conversation>, listener: ContactClickedListener,
                           private val phoneNumbers: List<String>) : ContactAdapter(conversations, listener) {

    override fun getLayoutId(): Int {
        return R.layout.invite_list_item
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val conversation = conversations[position]
        holder.checkBox.isChecked = phoneNumbers.contains(conversation.phoneNumbers)
    }

}
