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

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.listener.ContactClickedListener;

/**
 * Adapter for displaying a list of contacts. Each contact should be loaded into a conversation
 * object for easy use, the only fields that need filled are title, phoneNumbers and imageUri.
 */
public class ContactAdapter extends RecyclerView.Adapter<ConversationViewHolder> {

    private List<Conversation> conversations;
    private ContactClickedListener listener;
    private int lightToolbarTextColor = Integer.MIN_VALUE;

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

        if (lightToolbarTextColor == Integer.MIN_VALUE) {
            this.lightToolbarTextColor = parent.getContext().getResources().getColor(R.color.lightToolbarTextColor);
        }

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

        Settings settings = Settings.get(holder.itemView.getContext());
        if (conversation.imageUri == null || conversation.imageUri.isEmpty()) {
            if (settings.useGlobalThemeColor) {
                if (settings.mainColorSet.colorLight == Color.WHITE) {
                    holder.image.setImageDrawable(new ColorDrawable(settings.mainColorSet.colorDark));
                } else {
                    holder.image.setImageDrawable(new ColorDrawable(settings.mainColorSet.colorLight));
                }
            } else if (conversation.colors.color == Color.WHITE) {
                holder.image.setImageDrawable(new ColorDrawable(conversation.colors.colorDark));
            } else {
                holder.image.setImageDrawable(new ColorDrawable(conversation.colors.color));
            }

            int colorToInspect = settings.useGlobalThemeColor ? settings.mainColorSet.color : conversation.colors.color;
            if (ContactUtils.shouldDisplayContactLetter(conversation)) {
                holder.imageLetter.setText(conversation.title.substring(0, 1));
                if (holder.groupIcon.getVisibility() != View.GONE) {
                    holder.groupIcon.setVisibility(View.GONE);
                }

                if (ColorUtils.isColorDark(colorToInspect)) {
                    holder.imageLetter.setTextColor(Color.WHITE);
                } else {
                    holder.imageLetter.setTextColor(lightToolbarTextColor);
                }
            } else {
                holder.imageLetter.setText(null);
                if (holder.groupIcon.getVisibility() != View.VISIBLE) {
                    holder.groupIcon.setVisibility(View.VISIBLE);
                }

                if (conversation.phoneNumbers.contains(",")) {
                    holder.groupIcon.setImageResource(R.drawable.ic_group);
                } else {
                    holder.groupIcon.setImageResource(R.drawable.ic_person);
                }

                if (ColorUtils.isColorDark(colorToInspect)) {
                    holder.groupIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                } else {
                    holder.groupIcon.setImageTintList(ColorStateList.valueOf(lightToolbarTextColor));
                }
            }
        } else {
            if (!conversation.imageUri.endsWith("/photo")) {
                conversation.imageUri = conversation.imageUri + "/photo";
            }

            holder.imageLetter.setText(null);
            if (holder.groupIcon != null && holder.groupIcon.getVisibility() != View.GONE) {
                holder.groupIcon.setVisibility(View.GONE);
            }

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
