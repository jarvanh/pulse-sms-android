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

import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.data.Conversation;

/**
 * Adapter for displaying conversation items in a list. The adapter splits items into different
 * sections depending on whether they are pinned and when the last message was received.
 */
public class ConversationListAdapter extends SectionedRecyclerViewAdapter<ConversationViewHolder> {

    private static final int SECTION_PINNED = 0;
    private static final int SECTION_TODAY = 1;
    private static final int SECTION_YESTERDAY = 2;
    private static final int SECTION_OLDER = 3;

    private List<Conversation> conversations;
    private List<Integer> sectionCounts;

    public ConversationListAdapter(List<Conversation> conversations) {
        this.conversations = conversations;
        this.sectionCounts = new ArrayList<>();

        int currentSection = 0;
        int currentCount = 0;

        for (int i = 0; i < conversations.size(); i++) {
            Conversation conversation = conversations.get(i);

            // TODO improve logic here, it is checking for within 24 hours for the same day, but this isn't right.
            // TODO should this be expanded beyond just today, yesterday and older?
            if ((currentSection == SECTION_PINNED && conversation.pinned) ||
                    (currentSection == SECTION_TODAY && conversation.timestamp > System.currentTimeMillis() - (1000 * 60 * 60 * 24)) ||
                    (currentSection == SECTION_YESTERDAY && conversation.timestamp > System.currentTimeMillis() - (1000 * 60 * 60 * 48)) ||
                    (currentSection == SECTION_OLDER)) {
                currentCount++;
            } else {
                sectionCounts.add(currentCount);
                currentSection++;
                currentCount = 0;
                i--;
            }
        }

        sectionCounts.add(currentCount);
    }

    @Override
    public int getSectionCount() {
        return sectionCounts.size();
    }

    @Override
    public int getItemCount(int section) {
        return sectionCounts.get(section);
    }

    @Override
    public void onBindHeaderViewHolder(ConversationViewHolder holder, int section) {
        if (section == SECTION_PINNED) {
            holder.header.setText(R.string.pinned);
        } else if (section == SECTION_TODAY) {
            holder.header.setText(R.string.today);
        } else if (section == SECTION_YESTERDAY) {
            holder.header.setText(R.string.yesterday);
        } else {
            holder.header.setText(R.string.older);
        }
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int section, int relativePosition, int absolutePosition) {
        Conversation conversation = conversations.get(absolutePosition);

        holder.image.setImageDrawable(new ColorDrawable(conversation.contact.color));
        holder.name.setText(conversation.contact.name);
        holder.summary.setText(conversation.snippet);

        if (conversation.read && holder.name.getTypeface().isBold()) {
            holder.name.setTypeface(null, Typeface.NORMAL);
            holder.summary.setTypeface(null, Typeface.NORMAL);
        } else if (!conversation.read && !holder.name.getTypeface().isBold()) {
            holder.name.setTypeface(null, Typeface.BOLD);
            holder.summary.setTypeface(null, Typeface.BOLD);
        }
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType == VIEW_TYPE_HEADER ?
                        R.layout.conversation_list_header : R.layout.conversation_list_item,
                        parent, false);
        return new ConversationViewHolder(view);
    }

}
