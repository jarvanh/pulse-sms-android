package xyz.klinker.messenger.adapter.search

import android.view.View
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder

class SearchListHeaderBinder(private val adapter: SearchAdapter) {

    companion object {
        val SECTION_CONVERSATIONS = 0
        val SECTION_MESSAGES = 1
    }

    fun bind(holder: ConversationViewHolder, section: Int) {
        holder.header?.setText(if (section == SECTION_CONVERSATIONS) R.string.conversations else R.string.messages)
        holder.headerDone?.visibility = View.GONE
    }
}