package xyz.klinker.messenger.adapter

import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ListRow
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