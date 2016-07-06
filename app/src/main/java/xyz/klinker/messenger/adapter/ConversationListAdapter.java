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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.data.Conversation;
import xyz.klinker.messenger.data.SectionType;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeToDeleteListener;

/**
 * Adapter for displaying conversation items in a list. The adapter splits items into different
 * sections depending on whether they are pinned and when the last message was received.
 */
public class ConversationListAdapter extends SectionedRecyclerViewAdapter<ConversationViewHolder> {

    private List<Conversation> conversations;
    private SwipeToDeleteListener swipeToDeleteListener;
    private List<SectionType> sectionCounts;

    public ConversationListAdapter(List<Conversation> conversations,
                                   SwipeToDeleteListener listener) {
        this.swipeToDeleteListener = listener;
        setConversations(conversations);
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
        this.sectionCounts = new ArrayList<>();

        int currentSection = 0;
        int currentCount = 0;

        for (int i = 0; i < conversations.size(); i++) {
            Conversation conversation = conversations.get(i);

            // TODO improve logic here, it is checking for within 24 hours for the same day, but this isn't right.
            // TODO should this be expanded beyond just today, yesterday and older?
            if ((currentSection == SectionType.PINNED && conversation.pinned) ||
                    (currentSection == SectionType.TODAY && conversation.timestamp > System.currentTimeMillis() - (1000 * 60 * 60 * 24)) ||
                    (currentSection == SectionType.YESTERDAY && conversation.timestamp > System.currentTimeMillis() - (1000 * 60 * 60 * 48)) ||
                    (currentSection == SectionType.OLDER)) {
                currentCount++;
            } else {
                if (currentCount != 0) {
                    sectionCounts.add(new SectionType(currentSection, currentCount));
                }

                currentSection++;
                currentCount = 0;
                i--;
            }
        }

        sectionCounts.add(new SectionType(currentSection, currentCount));
    }

    @Override
    public int getSectionCount() {
        return sectionCounts.size();
    }

    @Override
    public int getItemCount(int section) {
        return sectionCounts.get(section).count;
    }

    @Override
    public void onBindHeaderViewHolder(ConversationViewHolder holder, int section) {
        if (sectionCounts.get(section).type == SectionType.PINNED) {
            holder.header.setText(R.string.pinned);
        } else if (sectionCounts.get(section).type == SectionType.TODAY) {
            holder.header.setText(R.string.today);
        } else if (sectionCounts.get(section).type == SectionType.YESTERDAY) {
            holder.header.setText(R.string.yesterday);
        } else {
            holder.header.setText(R.string.older);
        }
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int section, int relativePosition,
                                 int absolutePosition) {
        Conversation conversation = conversations.get(absolutePosition);

        holder.image.setImageDrawable(new ColorDrawable(conversation.contact.color));
        holder.name.setText(conversation.contact.name);
        holder.summary.setText(conversation.snippet);

        if (conversation.read && holder.isBold()) {
            holder.setBold(false);
        } else if (!conversation.read && !holder.isBold()) {
            holder.setBold(true);
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

    public void removeItem(int position) {
        // The logic here can get a little tricky because we are removing items from the adapter
        // but need to account for the headers taking up a position as well. On top of that, if all
        // the items in a section are gone, then there shouldn't be a header for that section.

        int originalPosition = position;
        int headersAbove = 1;
        int currentTotal = 0;

        for (SectionType type : sectionCounts) {
            currentTotal += type.count + 1; // +1 for the header above the section

            if (position < currentTotal) {
                position -= headersAbove;

                SectionType section = sectionCounts.get(headersAbove - 1);
                section.count -= 1;

                sectionCounts.set(headersAbove - 1, section);
                Conversation deletedConversation = conversations.remove(position);

                if (section.count == 0) {
                    sectionCounts.remove(headersAbove - 1);
                    notifyItemRangeRemoved(originalPosition - 1, 2);
                } else {
                    notifyItemRemoved(originalPosition);
                }

                swipeToDeleteListener.onSwipeToDelete(deletedConversation);

                break;
            } else {
                headersAbove++;
            }
        }
    }

}
