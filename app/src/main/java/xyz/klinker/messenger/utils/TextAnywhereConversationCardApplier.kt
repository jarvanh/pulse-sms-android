package xyz.klinker.messenger.utils

import xyz.klinker.messenger.adapter.ConversationListAdapter
import xyz.klinker.messenger.fragment.ConversationListFragment
import xyz.klinker.messenger.shared.data.SectionType

class TextAnywhereConversationCardApplier(val conversationList: ConversationListFragment) {

    private val adapter: ConversationListAdapter by lazy { conversationList.adapter }

    fun shouldAddCardToList(): Boolean {
        return adapter.sections[0].type != SectionType.CARD_ABOUT_ONLINE &&
                adapter.showHeaderAboutTextingOnline()
    }

    fun addCardToConversationList() {
        adapter.sections.add(0, SectionType(SectionType.CARD_ABOUT_ONLINE, 0))
        adapter.shouldShowHeadersForEmptySections(true)
        adapter.notifyItemInserted(0)
    }
}