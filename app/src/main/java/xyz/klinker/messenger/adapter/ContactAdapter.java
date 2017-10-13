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

package xyz.klinker.messenger.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
        if (conversation.getImageUri() == null || conversation.getImageUri().isEmpty()) {
            if (settings.useGlobalThemeColor) {
                if (Settings.INSTANCE.getMainColorSet().getColorLight()== Color.WHITE) {
                    holder.image.setImageDrawable(new ColorDrawable(Settings.INSTANCE.getMainColorSet().getColorDark()));
                } else {
                    holder.image.setImageDrawable(new ColorDrawable(Settings.INSTANCE.getMainColorSet().getColorLight()));
                }
            } else if (conversation.getColors().getColor() == Color.WHITE) {
                holder.image.setImageDrawable(new ColorDrawable(conversation.getColors().getColorDark()));
            } else {
                holder.image.setImageDrawable(new ColorDrawable(conversation.getColors().getColor()));
            }

            int colorToInspect = settings.useGlobalThemeColor ? Settings.INSTANCE.getMainColorSet().getColor() : conversation.getColors().getColor();
            if (ContactUtils.shouldDisplayContactLetter(conversation)) {
                holder.imageLetter.setText(conversation.getTitle().substring(0, 1));
                if (holder.groupIcon != null && holder.groupIcon.getVisibility() != View.GONE) {
                    holder.groupIcon.setVisibility(View.GONE);
                }

                if (ColorUtils.INSTANCE.isColorDark(colorToInspect)) {
                    holder.imageLetter.setTextColor(Color.WHITE);
                } else {
                    holder.imageLetter.setTextColor(lightToolbarTextColor);
                }
            } else {
                holder.imageLetter.setText(null);

                if (holder.groupIcon != null) {
                    if (holder.groupIcon.getVisibility() != View.VISIBLE) {
                        holder.groupIcon.setVisibility(View.VISIBLE);
                    }

                    if (conversation.getPhoneNumbers().contains(",")) {
                        holder.groupIcon.setImageResource(R.drawable.ic_group);
                    } else {
                        holder.groupIcon.setImageResource(R.drawable.ic_person);
                    }

                    if (ColorUtils.INSTANCE.isColorDark(colorToInspect)) {
                        holder.groupIcon.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    } else {
                        holder.groupIcon.setImageTintList(ColorStateList.valueOf(lightToolbarTextColor));
                    }
                }
            }
        } else {
            if (!conversation.getImageUri().endsWith("/photo")) {
                conversation.setImageUri(conversation.getImageUri() + "/photo");
            }

            holder.imageLetter.setText(null);
            if (holder.groupIcon != null && holder.groupIcon.getVisibility() != View.GONE) {
                holder.groupIcon.setVisibility(View.GONE);
            }

            Glide.with(holder.image.getContext())
                    .load(Uri.parse(conversation.getImageUri()))
                    .into(holder.image);
        }

        holder.name.setText(conversation.getTitle());
        holder.summary.setText(PhoneNumberUtils.format(conversation.getPhoneNumbers()));
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

}
