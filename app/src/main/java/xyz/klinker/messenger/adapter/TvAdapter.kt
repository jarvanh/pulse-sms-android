package xyz.klinker.messenger.adapter

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import xyz.klinker.messenger.fragment.message.MessageInstanceManager
import xyz.klinker.messenger.shared.data.model.Conversation

class TvAdapter(val conversations: List<Conversation>) : ArrayObjectAdapter() {

    override fun get(index: Int): Any {
        val conversation = conversations[index]

        val customFragmentAdapter = ArrayObjectAdapter()
        customFragmentAdapter.add(MessageInstanceManager.newInstance(conversation))

        return ListRow(HeaderItem(conversation.title), customFragmentAdapter)
    }
}