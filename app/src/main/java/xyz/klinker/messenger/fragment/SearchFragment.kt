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

package xyz.klinker.messenger.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.adapter.search.SearchAdapter
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.listener.SearchListener

/**
 * A fragment for searching through conversations and messages.
 */
class SearchFragment : Fragment(), SearchListener {

    private var query: String? = null

    private var list: RecyclerView? = null
    private val adapter: SearchAdapter by lazy { SearchAdapter(query, null, null, this) }

    val isSearching: Boolean
        get() = query != null && query!!.isNotEmpty()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, savedInstanceState: Bundle?): View? {
        list = inflater.inflate(R.layout.fragment_search, parent, false) as RecyclerView

        list?.layoutManager = LinearLayoutManager(activity)
        list?.adapter = adapter

        return list
    }

    fun search(query: String?) {
        this.query = query
        loadSearch()
    }

    private fun loadSearch() {
        val handler = Handler()

        Thread {
            val conversations = if (activity != null) {
                DataSource.searchConversationsAsList(activity, query, 60).toMutableList()
            } else mutableListOf()

            val messages = if (activity != null) {
                DataSource.searchMessagesAsList(activity, query, 60).toMutableList()
            } else mutableListOf()

            handler.post { setSearchResults(conversations, messages) }
        }.start()
    }

    private fun setSearchResults(conversations: MutableList<Conversation>, messages: MutableList<Message>) {
        adapter.updateCursors(query, conversations, messages)
    }

    override fun onSearchSelected(message: Message) {
        dismissKeyboard()

        DataSource.archiveConversation(activity!!, message.conversationId, false)

        val intent = Intent(activity, MessengerActivity::class.java)
        intent.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, message.conversationId)
        intent.putExtra(MessengerActivityExtras.EXTRA_MESSAGE_ID, message.id)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity!!.startActivity(intent)
    }

    override fun onSearchSelected(conversation: Conversation) {
        dismissKeyboard()

        if (conversation.archive) {
            DataSource.archiveConversation(activity, conversation.id, false)
        }

        val intent = Intent(activity, MessengerActivity::class.java)
        intent.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, conversation.id)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity?.startActivity(intent)
    }

    private fun dismissKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(list?.windowToken, 0)
    }

    companion object {
        fun newInstance(): SearchFragment {
            return SearchFragment()
        }
    }
}
