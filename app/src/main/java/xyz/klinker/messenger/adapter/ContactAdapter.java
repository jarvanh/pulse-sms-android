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
import xyz.klinker.messenger.util.PhoneNumberUtils;

/**
 * Adapter for displaying a list of contacts. Each contact should be loaded into a conversation
 * object for easy use, the only fields that need filled are title, phoneNumbers and imageUri.
 */
public class ContactAdapter extends RecyclerView.Adapter<ConversationViewHolder> {

    private List<Conversation> conversations;

    public ContactAdapter(List<Conversation> conversations) {
        this.conversations = conversations;
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contact_list_item, parent, false);
        return new ConversationViewHolder(view, null);
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);

        if (conversation.imageUri == null) {
            holder.image.setImageDrawable(new ColorDrawable(
                    ColorUtils.getRandomMaterialColor(holder.image.getContext()).color));
        } else {
            Glide.with(holder.image.getContext())
                    .load(Uri.parse(conversation.imageUri + "/photo"))
                    .into(holder.image);
        }

        holder.name.setText(conversation.title);
        holder.summary.setText(PhoneNumberUtils.format(conversation.phoneNumbers));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

}
