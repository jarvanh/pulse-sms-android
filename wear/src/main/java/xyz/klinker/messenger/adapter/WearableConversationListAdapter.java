package xyz.klinker.messenger.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.wearable.view.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.WearableConversationViewHolder;
import xyz.klinker.messenger.shared.data.SectionType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.pojo.ReorderType;
import xyz.klinker.messenger.shared.shared_interfaces.IConversationListAdapter;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.TimeUtils;

public class WearableConversationListAdapter extends SectionedRecyclerViewAdapter<WearableConversationViewHolder> implements IConversationListAdapter {

    private long time;

    private List<Conversation> conversations;
    private List<SectionType> sectionCounts;

    public WearableConversationListAdapter(List<Conversation> conversations) {
        setConversations(conversations);

        time = new Date().getTime();
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

    @Override
    public int getSectionCount() {
        return sectionCounts.size();
    }

    @Override
    public int getItemCount(int section) {
        return sectionCounts.get(section).count;
    }

    @Override
    public void onBindHeaderViewHolder(WearableConversationViewHolder holder, int section) {
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
    }

    @Override
    public void onViewRecycled(WearableConversationViewHolder holder){
        super.onViewRecycled(holder);

        if (holder.image != null) {
            Glide.clear(holder.image);
        }
    }

    @Override
    public void onBindViewHolder(WearableConversationViewHolder holder, int section, int relativePosition,
                                 int absolutePosition) {
        Conversation conversation = conversations.get(absolutePosition);

        // somehow a null conversation is being inserted in here sometimes after a new
        // conversation is created on the phone and the tablet gets a broadcast for it. Don't know
        // why this happens, but the situation is marked by a blank holder in the conversation list.
        if (conversation == null) {
            holder.conversation = null;
            holder.name.setText(null);
            holder.summary.setText(null);
            holder.imageLetter.setText(null);
            Glide.clear(holder.image);
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
                if (holder.groupIcon.getVisibility() != View.GONE) {
                    holder.groupIcon.setVisibility(View.GONE);
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
            }
        } else {
            holder.imageLetter.setText(null);
            if (holder.groupIcon.getVisibility() != View.GONE) {
                holder.groupIcon.setVisibility(View.GONE);
            }

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
    public WearableConversationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(viewType == VIEW_TYPE_HEADER ?
                                R.layout.item_conversation_header : R.layout.item_conversation,
                        parent, false);
        return new WearableConversationViewHolder(view);
    }

    @Override
    public int findPositionForConversationId(long id) {
        int headersAbove = 1;
        int conversationPosition = -1;

        for (int i = 0; i < conversations.size(); i++) {
            if (conversations.get(i) != null && conversations.get(i).id == id) {
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

    @Override
    public boolean removeItem(int position, ReorderType type) {
        return false;
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

    public List<SectionType> getSectionCounts() {
        return sectionCounts;
    }

}
