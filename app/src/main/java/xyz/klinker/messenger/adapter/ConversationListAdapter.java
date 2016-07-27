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

import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.SectionType;
import xyz.klinker.messenger.util.listener.ConversationExpandedListener;
import xyz.klinker.messenger.util.TimeUtils;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeToDeleteListener;

/**
 * Adapter for displaying conversation items in a list. The adapter splits items into different
 * sections depending on whether they are pinned and when the last message was received.
 */
public class ConversationListAdapter extends SectionedRecyclerViewAdapter<ConversationViewHolder> {

    private List<Conversation> conversations;
    private SwipeToDeleteListener swipeToDeleteListener;
    private ConversationExpandedListener conversationExpandedListener;
    private List<SectionType> sectionCounts;

    public ConversationListAdapter(Cursor conversations,
                                   SwipeToDeleteListener swipeToDeleteListener,
                                   ConversationExpandedListener conversationExpandedListener) {
        this.swipeToDeleteListener = swipeToDeleteListener;
        this.conversationExpandedListener = conversationExpandedListener;
        setConversations(conversations);
    }

    public void setConversations(Cursor cursor) {
        this.conversations = new ArrayList<>();
        this.sectionCounts = new ArrayList<>();

        int currentSection = 0;
        int currentCount = 0;

        if (cursor.moveToFirst()) {
            do {
                Conversation conversation = new Conversation();
                conversation.fillFromCursor(cursor);
                conversations.add(conversation);

                if ((currentSection == SectionType.PINNED && conversation.pinned) ||
                        (currentSection == SectionType.TODAY && TimeUtils.isToday(conversation.timestamp)) ||
                        (currentSection == SectionType.YESTERDAY && TimeUtils.isYesterday(conversation.timestamp)) ||
                        (currentSection == SectionType.OLDER)) {
                    currentCount++;
                } else {
                    if (currentCount != 0) {
                        sectionCounts.add(new SectionType(currentSection, currentCount));
                    }

                    currentSection++;
                    currentCount = 0;
                    cursor.moveToPrevious();
                    conversations.remove(conversation);
                }
            } while (cursor.moveToNext());
        }

        cursor.close();

        sectionCounts.add(new SectionType(currentSection, currentCount));
    }

    public List<SectionType> getSections() {
        return sectionCounts;
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
            holder.header.setText(holder.header.getContext().getString(R.string.pinned));
        } else if (sectionCounts.get(section).type == SectionType.TODAY) {
            holder.header.setText(holder.header.getContext().getString(R.string.today));
        } else if (sectionCounts.get(section).type == SectionType.YESTERDAY) {
            holder.header.setText(holder.header.getContext().getString(R.string.yesterday));
        } else {
            holder.header.setText(holder.header.getContext().getString(R.string.older));
        }
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int section, int relativePosition,
                                 int absolutePosition) {
        Conversation conversation = conversations.get(absolutePosition);

        holder.conversation = conversation;

        if (conversation.imageUri == null) {
            holder.image.setImageDrawable(new ColorDrawable(conversation.colors.color));
        } else {
            Glide.with(holder.image.getContext())
                    .load(Uri.parse(conversation.imageUri))
                    .into(holder.image);
        }

        holder.name.setText(conversation.title);
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
        return new ConversationViewHolder(view, conversationExpandedListener);
    }

    public void deleteItem(int position) {
        removeItem(position, true);
    }

    public void removeItem(int position, boolean delete) {
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

                if (delete) {
                    swipeToDeleteListener.onSwipeToDelete(deletedConversation);
                }

                break;
            } else {
                headersAbove++;
            }
        }
    }

    public int findPositionForConversationId(long conversationId) {
        int headersAbove = 1;
        int conversationPosition = -1;

        for (int i = 0; i < conversations.size(); i++) {
            if (conversations.get(i).id == conversationId) {
                conversationPosition = i;
                break;
            }
        }

        if (conversationPosition == -1) {
            return -1;
        }

        int totalSectionsCount = 0;

        for (int i = 0; i < sectionCounts.size(); i++) {
            totalSectionsCount += sectionCounts.get(i).count;

            if (conversationPosition < totalSectionsCount) {
                break;
            } else {
                headersAbove++;
            }
        }

        return conversationPosition + headersAbove;
    }

    public int getCountForSection(int sectionType) {
        for (int i = 0; i < sectionCounts.size(); i++) {
            if (sectionCounts.get(i).type == sectionType) {
                return sectionCounts.get(i).count;
            }
        }

        return 0;
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

    public List<SectionType> getSectionCounts() {
        return sectionCounts;
    }

}
