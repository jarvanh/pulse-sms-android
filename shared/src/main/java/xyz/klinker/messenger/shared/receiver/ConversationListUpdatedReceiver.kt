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

package xyz.klinker.messenger.shared.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.SectionType
import xyz.klinker.messenger.shared.data.pojo.ConversationUpdateInfo
import xyz.klinker.messenger.shared.data.pojo.ReorderType
import xyz.klinker.messenger.shared.shared_interfaces.IConversationListFragment

/**
 * Receiver that handles changing the conversation list when a new message is received. The logic
 * here can be quite tricky because of the pinned section and the section headers in the adapter.
 *
 *
 * We either need to create a new today section under the pinned section if one does not exist, or
 * we need to add an extra item to the today section and remove that item from below, depending
 * on whether we've received a new conversation or are just updating an old one.
 */
class ConversationListUpdatedReceiver(private val fragment: IConversationListFragment) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            handleReceiver(context, intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Throws(Exception::class)
    private fun handleReceiver(context: Context, intent: Intent) {
        if (!fragment.isFragmentAdded) {
            return
        }

        val conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1)
        val snippet = intent.getStringExtra(EXTRA_SNIPPET)
        val title = intent.getStringExtra(EXTRA_TITLE)
        val read = intent.getBooleanExtra(EXTRA_READ, false)

        if (conversationId == -1L || fragment.expandedId == conversationId || shouldIgnoreSnippet(snippet)) {
            return
        }

        val adapter = fragment.adapter ?: return

        val adapterPosition = adapter.findPositionForConversationId(conversationId)
        val insertToday = adapter.getCountForSection(SectionType.TODAY) == 0
        val pinnedCount = adapter.getCountForSection(SectionType.PINNED)

        val removeEmpty = adapter.conversations.isEmpty()

        if (adapterPosition == -1) {
            val conversation = DataSource.getConversation(context, conversationId)

            if (FeatureFlags.SECURE_PRIVATE && conversation?.privateNotifications == true) {
                // we don't want to add a conversation that is private...
                // if the fragment instance is a PrivateConversationListFragment, then we will always have the private conversations shown, and it wouldn't reach this
                // for any other type of fragment, we don't want to add the conversation there, since it is supposed to be password protected
                return
            }

            // todo: if it isn't the main conversation list fragment, then we should not ever add the conversation to the view...
            // This applies to private conversations, archives, and folders, in the future

            // need to insert after the pinned conversations
            if (conversation != null) {
                adapter.conversations.add(pinnedCount, conversation)
            } else return
        } else {
            val position = (0 until adapter.conversations.size).firstOrNull { adapter.conversations[it].id == conversationId } ?: return

            if (position <= pinnedCount) {
                // if it is already pinned or the top item that isn't pinned, just mark the read
                // and snippet changes

                if (title != null) {
                    adapter.conversations[position].title = title
                }

                if (snippet != null) {
                    adapter.conversations[position].snippet = snippet
                }

                if (intent.hasExtra(EXTRA_READ)) {
                    adapter.conversations[position].read = read
                }

                adapter.notifyItemChanged(adapterPosition)
                return
            } else {
                // remove, update, and reinsert conversation to appropriate place
                val conversation = adapter.conversations[position]
                adapter.removeItem(adapterPosition, ReorderType.NEITHER)

                if (title != null) {
                    conversation.title = title
                }

                if (snippet != null) {
                    conversation.snippet = snippet
                }

                if (intent.hasExtra(EXTRA_READ)) {
                    conversation.read = read
                }

                adapter.conversations.add(pinnedCount, conversation)
            }
        }

        if (insertToday) {
            // no today section exists, so we'll need to insert one. we need to check if pinned
            // conversations exist. if they do, then insert today in the second slot, if not then
            // insert it into the first slot.

            val type = SectionType(SectionType.TODAY, 1)
            if (pinnedCount == 0) {
                adapter.sectionCounts.add(0, type)
                adapter.notifyItemRangeInserted(0, 2)
            } else {
                adapter.sectionCounts.add(1, type)

                // add one to pinned count to include the header
                adapter.notifyItemRangeInserted(pinnedCount + 1, 2)
            }
        } else {
            if (pinnedCount == 0) {
                adapter.sectionCounts[0].count = adapter.sectionCounts[0].count + 1
                adapter.notifyItemInserted(1)
            } else {
                adapter.sectionCounts[1].count = adapter.sectionCounts[1].count + 1

                // add 2 here for the pinned header and today header
                adapter.notifyItemInserted(pinnedCount + 2)
            }
        }

        if (removeEmpty) {
            fragment.checkEmptyViewDisplay()
        }
    }

    fun shouldIgnoreSnippet(snippet: String?) = if (snippet!!.contains("img.youtube.com")) {
        true
    } else snippet.contains("{") && snippet.contains("}")

    companion object {

        private val ACTION_UPDATED = "xyz.klinker.messenger.CONVERSATION_UPDATED"
        private val EXTRA_CONVERSATION_ID = "conversation_id"
        private val EXTRA_SNIPPET = "snippet"
        private val EXTRA_TITLE = "title"
        private val EXTRA_READ = "read"

        /**
         * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
         */
        fun sendBroadcast(context: Context?, conversationId: Long, snippet: String?,
                          read: Boolean) {
            if (context == null) {
                return
            }

            var snippet = snippet
            try {
                if (snippet == null) {
                    val conversation = DataSource.getConversation(context, conversationId)
                    if (conversation != null) {
                        snippet = conversation.snippet
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val intent = Intent(ACTION_UPDATED)
            intent.putExtra(EXTRA_CONVERSATION_ID, conversationId)
            intent.putExtra(EXTRA_SNIPPET, snippet)
            intent.putExtra(EXTRA_READ, read)
            context.sendBroadcast(intent)
            Log.v("conversation_broadcast", "broadcasting conversation changes")
        }

        /**
         * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
         */
        fun sendBroadcast(context: Context?, conversationId: Long, title: String) {
            val intent = Intent(ACTION_UPDATED)
            intent.putExtra(EXTRA_CONVERSATION_ID, conversationId)
            intent.putExtra(EXTRA_TITLE, title)
            context?.sendBroadcast(intent)
            Log.v("conversation_broadcast", "broadcasting new title: " + title)
        }

        /**
         * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
         */
        fun sendBroadcast(context: Context?, updateInfo: ConversationUpdateInfo) {
            sendBroadcast(context, updateInfo.conversationId, updateInfo.snippet, updateInfo.read)
            Log.v("conversation_broadcast", "broadcasting new update info: " + updateInfo.snippet)
        }

        /**
         * Gets an intent filter that will pick up these broadcasts.
         */
        val intentFilter: IntentFilter
            get() = IntentFilter(ACTION_UPDATED)
    }
}
