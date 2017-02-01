package xyz.klinker.messenger.shared.shared_interfaces;

import java.util.List;

import xyz.klinker.messenger.shared.data.SectionType;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.pojo.ReorderType;

public interface IConversationListAdapter {

    int findPositionForConversationId(long id);
    int getCountForSection(int sectionType);
    boolean removeItem(int position, ReorderType type);

    void notifyItemChanged(int position);
    void notifyItemRangeInserted(int start, int end);
    void notifyItemInserted(int item);

    List<Conversation> getConversations();
    List<SectionType> getSectionCounts();
}
