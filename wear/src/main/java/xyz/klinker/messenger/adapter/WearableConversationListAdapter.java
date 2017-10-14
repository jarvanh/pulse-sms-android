package xyz.klinker.messenger.adapter;

import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;

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

            if ((currentSection == SectionType.Companion.getPINNED() && conversation.getPinned()) ||
                    (currentSection == SectionType.Companion.getTODAY() && TimeUtils.INSTANCE.isToday(conversation.getTimestamp())) ||
                    (currentSection == SectionType.Companion.getYESTERDAY() && TimeUtils.INSTANCE.isYesterday(conversation.getTimestamp())) ||
                    (currentSection == SectionType.Companion.getLAST_WEEK() && TimeUtils.INSTANCE.isLastWeek(conversation.getTimestamp())) ||
                    (currentSection == SectionType.Companion.getLAST_MONTH() && TimeUtils.INSTANCE.isLastMonth(conversation.getTimestamp())) ||
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

    @Override
    public int getSectionCount() {
        return sectionCounts.size();
    }

    @Override
    public int getItemCount(int section) {
        return sectionCounts.get(section).getCount();
    }

    @Override
    public void onBindHeaderViewHolder(WearableConversationViewHolder holder, int section) {
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
    }

    @Override
    public void onViewRecycled(WearableConversationViewHolder holder){
        super.onViewRecycled(holder);

        if (holder.image != null) {
            try {
                Glide.with(holder.image).clear(holder.image);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
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
            Glide.with(holder.image).clear(holder.image);
            return;
        }

        holder.conversation = conversation;
        holder.position = absolutePosition;

        if (conversation.getImageUri() == null || conversation.getImageUri().isEmpty()) {
            if (Settings.INSTANCE.getUseGlobalThemeColor()) {
                holder.image.setImageDrawable(new ColorDrawable(
                        Settings.INSTANCE.getMainColorSet().getColorLight()));
            } else {
                holder.image.setImageDrawable(new ColorDrawable(conversation.getColors().getColor()));
            }

            if (ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation)) {
                holder.imageLetter.setText(conversation.getTitle().substring(0, 1));
                if (holder.groupIcon.getVisibility() != View.GONE) {
                    holder.groupIcon.setVisibility(View.GONE);
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
        if (conversation.getRead() && conversation.getMute() && (holder.isBold() || !holder.isItalic())) {
            // should be italic
            holder.setTypeface(false, true);
        } else if (conversation.getMute() && !conversation.getRead() && (!holder.isItalic() || !holder.isBold())) {
            // should be just italic
            holder.setTypeface(true, true);
        } else if (!conversation.getMute() && conversation.getRead() && (holder.isItalic() || holder.isBold())) {
            // should be not italic and not bold
            holder.setTypeface(false, false);
        } else if (!conversation.getMute() && !conversation.getRead() && (holder.isItalic() || !holder.isBold())) {
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
            if (conversations.get(i) != null && conversations.get(i).getId() == id) {
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

    public int getCountForSection(int sectionType) {
        for (int i = 0; i < sectionCounts.size(); i++) {
            if (sectionCounts.get(i).getType() == sectionType) {
                return sectionCounts.get(i).getCount();
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
