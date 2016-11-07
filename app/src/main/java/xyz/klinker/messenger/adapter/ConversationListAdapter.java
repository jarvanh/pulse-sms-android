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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.data.SectionType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.fragment.ArchivedConversationListFragment;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.TimeUtils;
import xyz.klinker.messenger.util.listener.ConversationExpandedListener;
import xyz.klinker.messenger.util.multi_select.ConversationsMultiSelectDelegate;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeToDeleteListener;

/**
 * Adapter for displaying conversation items in a list. The adapter splits items into different
 * sections depending on whether they are pinned and when the last message was received.
 */
public class ConversationListAdapter extends SectionedRecyclerViewAdapter<ConversationViewHolder> {

    public enum ReorderType {
        DELETE, ARCHIVE, NEITHER
    }

    private long time;

    private List<Conversation> conversations;
    private SwipeToDeleteListener swipeToDeleteListener;
    private ConversationExpandedListener conversationExpandedListener;
    private List<SectionType> sectionCounts;
    private ConversationsMultiSelectDelegate multiSelector;

    public ConversationListAdapter(List<Conversation> conversations, ConversationsMultiSelectDelegate multiSelector,
                                   SwipeToDeleteListener swipeToDeleteListener,
                                   ConversationExpandedListener conversationExpandedListener) {
        this.swipeToDeleteListener = swipeToDeleteListener;
        this.conversationExpandedListener = conversationExpandedListener;
        setConversations(conversations);

        time = new Date().getTime();

        this.multiSelector = multiSelector;
        this.multiSelector.setAdapter(this);
    }

    public void setConversations(List<Conversation> convos) {
        this.conversations = new ArrayList<>();
        this.sectionCounts = new ArrayList<>();

        int currentSection = 0;
        int currentCount = 0;

        for (int i = 0; i < convos.size(); i++) {
            Conversation conversation = convos.get(i);
            this.conversations.add(conversation);

            if ((currentSection == SectionType.PINNED && conversation.pinned) ||
                    (currentSection == SectionType.TODAY && TimeUtils.isToday(conversation.timestamp)) ||
                    (currentSection == SectionType.YESTERDAY && TimeUtils.isYesterday(conversation.timestamp)) ||
                    (currentSection == SectionType.LAST_WEEK && TimeUtils.isLastWeek(conversation.timestamp)) ||
                    (currentSection == SectionType.LAST_MONTH && TimeUtils.isLastMonth(conversation.timestamp)) ||
                    (currentSection == SectionType.OLDER)) {
                currentCount++;
            } else {
                if (currentCount != 0) {
                    sectionCounts.add(new SectionType(currentSection, currentCount));
                }

                currentSection++;
                currentCount = 0;
                i--;
                this.conversations.remove(conversation);
            }
        }

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
        String text = null;

        if (sectionCounts.get(section).type == SectionType.PINNED) {
            text = holder.header.getContext().getString(R.string.pinned);
        } else if (sectionCounts.get(section).type == SectionType.TODAY) {
            text = holder.header.getContext().getString(R.string.today);
        } else if (sectionCounts.get(section).type == SectionType.YESTERDAY) {
            text = holder.header.getContext().getString(R.string.yesterday);
        } else if (sectionCounts.get(section).type == SectionType.LAST_WEEK) {
            text = holder.header.getContext().getString(R.string.last_week);
        } else if (sectionCounts.get(section).type == SectionType.LAST_MONTH) {
            text = holder.header.getContext().getString(R.string.last_month);
        } else {
            text = holder.header.getContext().getString(R.string.older);
        }

        holder.header.setText(text);

        if (holder.headerDone != null) {
            holder.headerDone.setOnClickListener(
                    getHeaderDoneClickListener(text, sectionCounts.get(section).type)
            );
            holder.headerDone.setOnLongClickListener(
                    getHeaderDoneLongClickListener(text)
            );
        }
    }

    @Override
    public void onViewRecycled(ConversationViewHolder holder){
        super.onViewRecycled(holder);

        if (holder.image != null) {
            Glide.clear(holder.image);
        }
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int section, int relativePosition,
                                 int absolutePosition) {
        Conversation conversation = conversations.get(absolutePosition);

        // somehow a null conversation is being inserted in here sometimes after a new
        // conversation is created on the phone and the tablet gets a broadcast for it. Don't know
        // why this happens, but the situation is marked by a blank holder in the conversation list.
        if (conversation == null) {
            holder.conversation = null;
            holder.image.setImageDrawable(null);
            holder.name.setText(null);
            holder.summary.setText(null);
            holder.imageLetter.setText(null);
            return;
        }

        holder.conversation = conversation;
        holder.position = absolutePosition;

        if (conversation.imageUri == null || conversation.imageUri.isEmpty()) {
            if (Settings.get(holder.itemView.getContext()).useGlobalThemeColor) {
                holder.image.setImageDrawable(new ColorDrawable(
                        Settings.get(holder.itemView.getContext()).globalColorSet.colorLight));
            } else {
                holder.image.setImageDrawable(new ColorDrawable(conversation.colors.color));
            }

            if (ContactUtils.shouldDisplayContactLetter(conversation)) {
                holder.imageLetter.setText(conversation.title.substring(0, 1));
            } else {
                holder.imageLetter.setText(null);
            }
        } else {
            holder.imageLetter.setText(null);
            Glide.with(holder.image.getContext())
                    .load(Uri.parse(conversation.imageUri))
                    .signature(new StringSignature(String.valueOf(time)))
                    .into(holder.image);
        }

        holder.name.setText(conversation.title);
        if (conversation.privateNotifications || conversation.snippet == null ||
                conversation.snippet.contains("file://") || conversation.snippet.contains("content://")) {
            holder.summary.setText("");
        } else {
            holder.summary.setText(conversation.snippet);
        }

        // read not muted
        // not read, not muted
        // muted not read
        // read and muted
        if (conversation.read && conversation.mute && (holder.isBold() || !holder.isItalic())) {
            // should be italic
            holder.setTypeface(false, true);
        } else if (conversation.mute && !conversation.read && (!holder.isItalic() || !holder.isBold())) {
            // should be just italic
            holder.setTypeface(true, true);
        } else if (!conversation.mute && conversation.read && (holder.isItalic() || holder.isBold())) {
            // should be not italic and not bold
            holder.setTypeface(false, false);
        } else if (!conversation.mute && !conversation.read && (holder.isItalic() || !holder.isBold())) {
            // should be bold, not italic
            holder.setTypeface(true, false);
        }
    }

    @Override
    public ConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType == VIEW_TYPE_HEADER ?
                                R.layout.conversation_list_header : R.layout.conversation_list_item,
                        parent, false);
        return new ConversationViewHolder(view, conversationExpandedListener, this);
    }

    public void deleteItem(int position) {
        removeItem(position, ReorderType.DELETE);
    }

    public void archiveItem(int position) {
        removeItem(position, ReorderType.ARCHIVE);
    }

    public void removeItem(int position, ReorderType reorderType) {
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

                if (reorderType == ReorderType.DELETE) {
                    swipeToDeleteListener.onSwipeToDelete(deletedConversation);
                } else if (reorderType == ReorderType.ARCHIVE) {
                    swipeToDeleteListener.onSwipeToArchive(deletedConversation);
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
            if (conversations.get(i) != null && conversations.get(i).id == conversationId) {
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

    public Conversation findConversationForPosition(int position) {
        int headersAbove = sectionCounts.get(0).count != 0 ? 1 : 0; // on archives there may not be a pinned section
        int totalSectionsCount = 0;

        for (int i = 0; i < sectionCounts.size(); i++) {
            totalSectionsCount += sectionCounts.get(i).count;

            if (position <= (totalSectionsCount + headersAbove)) {
                break;
            } else {
                headersAbove++;
            }
        }

        return conversations.get(position - headersAbove);
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

    private View.OnClickListener getHeaderDoneClickListener(final String text, final int sectionType) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swipeToDeleteListener.onMarkSectionAsRead(text, sectionType);
            }
        };
    }

    private View.OnLongClickListener getHeaderDoneLongClickListener(final String text) {
        return new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                swipeToDeleteListener.onShowMarkAsRead(text);
                return false;
            }
        };
    }

    public ConversationsMultiSelectDelegate getMultiSelector() {
        return multiSelector;
    }

}
