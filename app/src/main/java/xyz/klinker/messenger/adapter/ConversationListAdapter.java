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
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.firebase.AnalyticsHelper;
import xyz.klinker.messenger.shared.data.SectionType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.pojo.ReorderType;
import xyz.klinker.messenger.shared.shared_interfaces.IConversationListAdapter;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.TimeUtils;
import xyz.klinker.messenger.utils.listener.ConversationExpandedListener;
import xyz.klinker.messenger.utils.multi_select.ConversationsMultiSelectDelegate;
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeToDeleteListener;

/**
 * Adapter for displaying conversation items in a list. The adapter splits items into different
 * sections depending on whether they are pinned and when the last message was received.
 */
public class ConversationListAdapter extends SectionedRecyclerViewAdapter<ConversationViewHolder> implements IConversationListAdapter {

    private long time;

    private MessengerActivity activity;
    private int lightToolbarTextColor;

    private List<Conversation> conversations;
    private SwipeToDeleteListener swipeToDeleteListener;
    private ConversationExpandedListener conversationExpandedListener;
    private List<SectionType> sectionCounts;
    private ConversationsMultiSelectDelegate multiSelector;

    public ConversationListAdapter(MessengerActivity context, List<Conversation> conversations, ConversationsMultiSelectDelegate multiSelector,
                                   SwipeToDeleteListener swipeToDeleteListener,
                                   ConversationExpandedListener conversationExpandedListener) {
        this.activity = context;
        this.lightToolbarTextColor = context != null && context.getResources() != null ? context.getResources().getColor(R.color.lightToolbarTextColor) :
                Color.parseColor("#444444");

        this.swipeToDeleteListener = swipeToDeleteListener;
        this.conversationExpandedListener = conversationExpandedListener;
        setConversations(conversations);

        time = new Date().getTime();

        this.multiSelector = multiSelector;
        if (this.multiSelector != null)
            this.multiSelector.setAdapter(this);

        shouldShowHeadersForEmptySections(showHeaderAboutTextingOnline());
    }

    public void setConversations(List<Conversation> convos) {
        this.conversations = new ArrayList<>();
        this.sectionCounts = new ArrayList<>();

        if (showHeaderAboutTextingOnline()) {
            sectionCounts.add(new SectionType(SectionType.Companion.getCARD_ABOUT_ONLINE(), 0));
            AnalyticsHelper.convoListCardShown(activity);
        }

        int currentSection = 0;
        int currentCount = 0;

        for (int i = 0; i < convos.size(); i++) {
            Conversation conversation = convos.get(i);
            this.conversations.add(conversation);

            if ((currentSection == SectionType.Companion.getPINNED() && conversation.getPinned()) ||
                    (currentSection == SectionType.Companion.getTODAY() && TimeUtils.isToday(conversation.getTimestamp())) ||
                    (currentSection == SectionType.Companion.getYESTERDAY() && TimeUtils.isYesterday(conversation.getTimestamp())) ||
                    (currentSection == SectionType.Companion.getLAST_WEEK() && TimeUtils.isLastWeek(conversation.getTimestamp())) ||
                    (currentSection == SectionType.Companion.getLAST_MONTH() && TimeUtils.isLastMonth(conversation.getTimestamp())) ||
                    (currentSection == SectionType.Companion.getOLDER())) {
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
        return sectionCounts.get(section).getCount();
    }

    @Override
    public void onBindHeaderViewHolder(ConversationViewHolder holder, int section) {
        if (sectionCounts.get(section).getType() == SectionType.Companion.getCARD_ABOUT_ONLINE()) {
            if (holder.header.getVisibility() != View.GONE)
                holder.header.setVisibility(View.GONE);
            if (holder.headerDone.getVisibility() != View.GONE)
                holder.headerDone.setVisibility(View.GONE);
            if (holder.headerCardForTextOnline.getVisibility() != View.VISIBLE)
                holder.headerCardForTextOnline.setVisibility(View.VISIBLE);

            TextView tryIt = (TextView) holder.headerCardForTextOnline.findViewById(R.id.try_it);
            tryIt.setTextColor(Settings.get(activity).mainColorSet.getColor());

            tryIt.setOnClickListener(v -> {
                if (sectionCounts.size() > 0) {
                    sectionCounts.remove(0);
                }

                Settings.get(activity).setValue(activity, activity.getString(R.string.pref_show_text_online_on_conversation_list), false);
                notifyItemRemoved(0);

                tryIt.postDelayed(() -> {
                    activity.menuItemClicked(R.id.drawer_account);
                    activity.clickNavigationItem(R.id.drawer_account);
                    AnalyticsHelper.convoListTryIt(activity);
                }, 500);
            });
            holder.headerCardForTextOnline.findViewById(R.id.not_now).setOnClickListener(v -> {
                if (sectionCounts.size() > 0) {
                    sectionCounts.remove(0);
                }

                Settings.get(activity).setValue(activity, activity.getString(R.string.pref_show_text_online_on_conversation_list), false);
                notifyItemRemoved(0);
                AnalyticsHelper.convoListNotNow(activity);
            });
        } else {
            if (holder.header.getVisibility() != View.VISIBLE)
                holder.header.setVisibility(View.VISIBLE);
            if (holder.headerDone.getVisibility() != View.VISIBLE)
                holder.headerDone.setVisibility(View.VISIBLE);
            if (holder.headerCardForTextOnline.getVisibility() != View.GONE)
                holder.headerCardForTextOnline.setVisibility(View.GONE);

            String text;
            if (sectionCounts.get(section).getType() == SectionType.Companion.getPINNED()) {
                text = holder.header.getContext().getString(R.string.pinned);
            } else if (sectionCounts.get(section).getType() == SectionType.Companion.getTODAY()) {
                text = holder.header.getContext().getString(R.string.today);
            } else if (sectionCounts.get(section).getType() == SectionType.Companion.getYESTERDAY()) {
                text = holder.header.getContext().getString(R.string.yesterday);
            } else if (sectionCounts.get(section).getType() == SectionType.Companion.getLAST_WEEK()) {
                text = holder.header.getContext().getString(R.string.last_week);
            } else if (sectionCounts.get(section).getType() == SectionType.Companion.getLAST_MONTH()) {
                text = holder.header.getContext().getString(R.string.last_month);
            } else {
                text = holder.header.getContext().getString(R.string.older);
            }

            holder.header.setText(text);

            if (holder.headerDone != null) {
                holder.headerDone.setOnClickListener(
                        getHeaderDoneClickListener(text, sectionCounts.get(section).getType())
                );
                holder.headerDone.setOnLongClickListener(
                        getHeaderDoneLongClickListener(text)
                );
            }
        }
    }

    @Override
    public void onViewRecycled(ConversationViewHolder holder) {
        super.onViewRecycled(holder);

        if (holder.image != null) {
            try {
                Glide.with(holder.image.getContext()).clear(holder.image);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBindViewHolder(ConversationViewHolder holder, int section, int relativePosition,
                                 int absolutePosition) {
        if (absolutePosition >= conversations.size() || absolutePosition < 0) {
            return;
        }

        Conversation conversation = conversations.get(absolutePosition);

        // somehow a null conversation is being inserted in here sometimes after a new
        // conversation is created on the phone and the tablet gets a broadcast for it. Don't know
        // why this happens, but the situation is marked by a blank holder in the conversation list.
        if (conversation == null) {
            holder.conversation = null;
            holder.name.setText(null);
            holder.summary.setText(null);
            holder.imageLetter.setText(null);
            Glide.with(holder.image.getContext()).clear(holder.image);
            return;
        }

        holder.conversation = conversation;
        holder.position = absolutePosition;

        Settings settings = Settings.get(holder.itemView.getContext());
        if (conversation.getImageUri() == null || conversation.getImageUri().isEmpty()) {
            if (settings.useGlobalThemeColor) {
                if (Settings.INSTANCE.getMainColorSet().getColorLight() == Color.WHITE) {
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
                if (holder.groupIcon.getVisibility() != View.GONE) {
                    holder.groupIcon.setVisibility(View.GONE);
                }

                if (ColorUtils.INSTANCE.isColorDark(colorToInspect)) {
                    holder.imageLetter.setTextColor(Color.WHITE);
                } else {
                    holder.imageLetter.setTextColor(lightToolbarTextColor);
                }
            } else {
                holder.imageLetter.setText(null);
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
        } else {
            holder.imageLetter.setText(null);
            if (holder.groupIcon.getVisibility() != View.GONE) {
                holder.groupIcon.setVisibility(View.GONE);
            }

            Glide.with(holder.image.getContext())
                    .load(Uri.parse(conversation.getImageUri()))
                    .into(holder.image);
        }

        holder.name.setText(conversation.getTitle());
        if (conversation.getPrivateNotifications() || conversation.getSnippet() == null ||
                conversation.getSnippet().contains("file://") || conversation.getSnippet().contains("content://")) {
            holder.summary.setText("");
        } else {
            holder.summary.setText(conversation.getSnippet());
        }

        // read not muted
        // not read, not muted
        // muted not read
        // read and muted
//        if (conversation.read && conversation.mute && (holder.isBold() || !holder.isItalic())) {
        if (conversation.getRead() && conversation.getMute()) {
            // should be italic
            holder.setTypeface(false, true);
//        } else if (conversation.mute && !conversation.read && (!holder.isItalic() || !holder.isBold())) {
        } else if (conversation.getMute() && !conversation.getRead()) {
            // should be just italic
            holder.setTypeface(true, true);
//        } else if (!conversation.mute && conversation.read && (holder.isItalic() || holder.isBold())) {
        } else if (!conversation.getMute() && conversation.getRead()) {
            // should be not italic and not bold
            holder.setTypeface(false, false);
//        } else if (!conversation.mute && !conversation.read && (holder.isItalic() || !holder.isBold())) {
        } else if (!conversation.getMute() && !conversation.getRead()) {
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

    public boolean deleteItem(int position) {
        return removeItem(position, ReorderType.DELETE);
    }

    public boolean archiveItem(int position) {
        return removeItem(position, ReorderType.ARCHIVE);
    }

    public boolean removeItem(int position, ReorderType reorderType) {
        if (position == -1) {
            return false;
        }

        // The logic here can get a little tricky because we are removing items from the adapter
        // but need to account for the headers taking up a position as well. On top of that, if all
        // the items in a section are gone, then there shouldn't be a header for that section.

        boolean removedHeader = false;

        int originalPosition = position;
        int headersAbove = 1;
        int currentTotal = 0;

        for (SectionType type : sectionCounts) {
            currentTotal += type.getCount() + 1; // +1 for the header above the section

            if (position < currentTotal) {
                position -= headersAbove;

                SectionType section = sectionCounts.get(headersAbove - 1);
                section.setCount(section.getCount() - 1);

                sectionCounts.set(headersAbove - 1, section);
                Conversation deletedConversation = conversations.remove(position);

                if (section.getCount() == 0) {
                    sectionCounts.remove(headersAbove - 1);
                    notifyItemRangeRemoved(originalPosition - 1, 2);
                    removedHeader = true;
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

        return removedHeader;
    }

    public int findPositionForConversationId(long conversationId) {
        int headersAbove = 1;
        int conversationPosition = -1;

        for (int i = 0; i < conversations.size(); i++) {
            if (conversations.get(i) != null && conversations.get(i).getId() == conversationId) {
                conversationPosition = i;
                break;
            }
        }

        if (conversationPosition == -1) {
            return -1;
        }

        int totalSectionsCount = 0;

        for (int i = 0; i < sectionCounts.size(); i++) {
            totalSectionsCount += sectionCounts.get(i).getCount();

            if (conversationPosition < totalSectionsCount) {
                break;
            } else {
                headersAbove++;
            }
        }

        return conversationPosition + headersAbove;
    }

    public Conversation findConversationForPosition(int position) {
        int headersAbove = 0;
        int totalSectionsCount = 0;

        for (int i = 0; i < sectionCounts.size(); i++) {
            totalSectionsCount += sectionCounts.get(i).getCount();

            if (position <= (totalSectionsCount + headersAbove)) {
                headersAbove++;
                break;
            } else {
                if (sectionCounts.get(i).getCount() != 0) {
                    // only add the header if it has more than 0 items, otherwise it isn't shown
                    headersAbove++;
                }
            }
        }

        int convoPosition = position - headersAbove;

        if (convoPosition == conversations.size()) {
            return conversations.get(convoPosition - 1);
        } else {
            return conversations.get(convoPosition);
        }
    }

    public int getCountForSection(int sectionType) {
        for (int i = 0; i < sectionCounts.size(); i++) {
            if (sectionCounts.get(i).getType() == sectionType) {
                return sectionCounts.get(i).getCount();
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
        return v -> swipeToDeleteListener.onMarkSectionAsRead(text, sectionType);
    }

    private View.OnLongClickListener getHeaderDoneLongClickListener(final String text) {
        return v -> {
            swipeToDeleteListener.onShowMarkAsRead(text);
            return false;
        };
    }

    public ConversationsMultiSelectDelegate getMultiSelector() {
        return multiSelector;
    }

    public boolean showHeaderAboutTextingOnline() {
        if (Build.FINGERPRINT.equals("robolectric")) {
            return false;
        } else {
            Settings settings = Settings.get(activity);
            return !Account.INSTANCE.exists() &&
                    settings.showTextOnlineOnConversationList &&
                    Math.abs(settings.installTime - new Date().getTime()) > TimeUtils.MINUTE * 15;
        }
    }
}
