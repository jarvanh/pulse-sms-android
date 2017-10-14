package xyz.klinker.messenger.shared.shared_interfaces

import xyz.klinker.messenger.shared.data.SectionType
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.pojo.ReorderType

interface IConversationListAdapter {

    val conversations: List<Conversation>
    val sectionCounts: List<SectionType>

    fun findPositionForConversationId(id: Long): Int
    fun getCountForSection(sectionType: Int): Int
    fun removeItem(position: Int, type: ReorderType): Boolean

    fun notifyItemChanged(position: Int)
    fun notifyItemRangeInserted(start: Int, end: Int)
    fun notifyItemInserted(item: Int)
}
