/*
 * Copyright (C) 2016 Jacob Klinker
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

package xyz.klinker.messenger.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.listener.ContactClickedListener;

/**
 * Adds a checkbox to the base contact adapter so that you can select multiple items.
 */
public class InviteFriendsAdapter extends ContactAdapter {

    private List<String> phoneNumbers;

    public InviteFriendsAdapter(List<Conversation> conversations, ContactClickedListener listener,
                                List<String> phoneNumbers) {
        super(conversations, listener);
        this.phoneNumbers = phoneNumbers;
    }

    @Override
    public int getLayoutId() {
        return R.layout.invite_list_item;
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        Conversation conversation = getConversations().get(position);
        if (phoneNumbers.contains(conversation.phoneNumbers)) {
            holder.checkBox.setChecked(true);
        } else {
            holder.checkBox.setChecked(false);
        }
    }

}
