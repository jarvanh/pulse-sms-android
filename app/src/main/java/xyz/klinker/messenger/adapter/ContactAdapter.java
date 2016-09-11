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

import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.listener.ContactClickedListener;

/**
 * Adapter for displaying a list of contacts. Each contact should be loaded into a conversation
 * object for easy use, the only fields that need filled are title, phoneNumbers and imageUri.
 */
public class ContactAdapter extends RecyclerView.Adapter<ConversationViewHolder> {

    private List<Conversation> conversations;
    private ContactClickedListener listener;

    public ContactAdapter(List<Conversation> conversations, ContactClickedListener listener) {
        this.conversations = conversations;
        this.listener = listener;
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(getLayoutId(), parent, false);
        ConversationViewHolder holder = new ConversationViewHolder(view, null, null);
        holder.setContactClickedListener(listener);
        return holder;
    }

    @LayoutRes
    public int getLayoutId() {
        return R.layout.contact_list_item;
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);

        holder.conversation = conversation;

        if (conversation.imageUri == null) {
            if (conversation.colors.color != 0) {
                holder.image.setImageDrawable(new ColorDrawable(conversation.colors.color));
            } else {
                holder.image.setImageDrawable(new ColorDrawable(
                        ColorUtils.getRandomMaterialColor(holder.image.getContext()).color));
            }

            if (ContactUtils.shouldDisplayContactLetter(conversation)) {
                holder.imageLetter.setText(conversation.title.substring(0, 1));
            } else {
                holder.imageLetter.setText(null);
            }
        } else {
            if (!conversation.imageUri.endsWith("/photo")) {
                conversation.imageUri = conversation.imageUri + "/photo";
            }

            holder.imageLetter.setText(null);

            Glide.with(holder.image.getContext())
                    .load(Uri.parse(conversation.imageUri))
                    .into(holder.image);
        }

        holder.name.setText(conversation.title);
        holder.summary.setText(PhoneNumberUtils.format(conversation.phoneNumbers));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

}
