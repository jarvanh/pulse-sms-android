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

package xyz.klinker.messenger.shared.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.util.List;

import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.SectionType;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.pojo.ConversationUpdateInfo;
import xyz.klinker.messenger.shared.data.pojo.ReorderType;
import xyz.klinker.messenger.shared.shared_interfaces.IConversationListAdapter;
import xyz.klinker.messenger.shared.shared_interfaces.IConversationListFragment;

/**
 * Receiver that handles changing the conversation list when a new message is received. The logic
 * here can be quite tricky because of the pinned section and the section headers in the adapter.
 * <p>
 * We either need to create a new today section under the pinned section if one does not exist, or
 * we need to add an extra item to the today section and remove that item from below, depending
 * on whether we've received a new conversation or are just updating an old one.
 */
public class ConversationListUpdatedReceiver extends BroadcastReceiver {

    private static final String ACTION_UPDATED = "xyz.klinker.messenger.CONVERSATION_UPDATED";
    private static final String EXTRA_CONVERSATION_ID = "conversation_id";
    private static final String EXTRA_SNIPPET = "snippet";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_READ = "read";

    private IConversationListFragment fragment;

    public ConversationListUpdatedReceiver(IConversationListFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            handleReceiver(context, intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleReceiver(Context context, Intent intent) throws Exception {
        if (!fragment.isAdded()) {
            return;
        }

        long conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1);
        String snippet = intent.getStringExtra(EXTRA_SNIPPET);
        String title = intent.getStringExtra(EXTRA_TITLE);
        boolean read = intent.getBooleanExtra(EXTRA_READ, false);

        if (conversationId == -1 || fragment.getExpandedId() == conversationId || shouldIgnoreSnippet(snippet)) {
            return;
        }

        IConversationListAdapter adapter = fragment.getAdapter();
        int adapterPosition = adapter.findPositionForConversationId(conversationId);
        boolean insertToday = adapter.getCountForSection(SectionType.Companion.getTODAY()) == 0;
        int pinnedCount = adapter.getCountForSection(SectionType.Companion.getPINNED());
        List<Conversation> conversations = adapter.getConversations();
        List<SectionType> sectionTypes = adapter.getSectionCounts();

        boolean removeEmpty = conversations.size() == 0;

        if (adapterPosition == -1) {
            Conversation conversation = DataSource.INSTANCE.getConversation(context, conversationId);

            // need to insert after the pinned conversations
            conversations.add(pinnedCount, conversation);
        } else {
            int position = -1;
            for (int i = 0; i < conversations.size(); i++) {
                if (conversations.get(i).getId() == conversationId) {
                    position = i;
                    break;
                }
            }

            if (position == -1) {
                return;
            }

            if (position <= pinnedCount) {
                // if it is already pinned or the top item that isn't pinned, just mark the read
                // and snippet changes
                Conversation conversation = conversations.get(position);

                if (title != null) {
                    conversation.setTitle(title);
                }

                if (snippet != null) {
                    conversation.setSnippet(snippet);
                }

                if (intent.hasExtra(EXTRA_READ)) {
                    conversation.setRead(read);
                }

                adapter.notifyItemChanged(adapterPosition);
                return;
            } else {
                // remove, update, and reinsert conversation to appropriate place
                Conversation conversation = conversations.get(position);
                adapter.removeItem(adapterPosition, ReorderType.NEITHER);

                if (title != null) {
                    conversation.setTitle(title);
                }

                if (snippet != null) {
                    conversation.setSnippet(snippet);
                }

                if (intent.hasExtra(EXTRA_READ)) {
                    conversation.setRead(read);
                }

                conversations.add(pinnedCount, conversation);
            }
        }

        if (insertToday) {
            // no today section exists, so we'll need to insert one. we need to check if pinned
            // conversations exist. if they do, then insert today in the second slot, if not then
            // insert it into the first slot.

            SectionType type = new SectionType(SectionType.Companion.getTODAY(), 1);
            if (pinnedCount == 0) {
                sectionTypes.add(0, type);
                adapter.notifyItemRangeInserted(0, 2);
            } else {
                sectionTypes.add(1, type);

                // add one to pinned count to include the header
                adapter.notifyItemRangeInserted(pinnedCount + 1, 2);
            }
        } else {
            if (pinnedCount == 0) {
                sectionTypes.get(0).setCount(sectionTypes.get(0).getCount() + 1);
                adapter.notifyItemInserted(1);
            } else {
                sectionTypes.get(1).setCount(sectionTypes.get(1).getCount() + 1);

                // add 2 here for the pinned header and today header
                adapter.notifyItemInserted(pinnedCount + 2);
            }
        }

        if (removeEmpty) {
            fragment.checkEmptyViewDisplay();
        }
    }

    /**
     * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
     */
    public static void sendBroadcast(Context context, long conversationId, String snippet,
                                     boolean read) {
        try {
            if (snippet == null) {
                Conversation conversation = DataSource.INSTANCE.getConversation(context, conversationId);
                if (conversation != null) {
                    snippet = conversation.getSnippet();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(ACTION_UPDATED);
        intent.putExtra(EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(EXTRA_SNIPPET, snippet);
        intent.putExtra(EXTRA_READ, read);
        context.sendBroadcast(intent);
        Log.v("conversation_broadcast", "broadcasting conversation changes");
    }

    /**
     * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
     */
    public static void sendBroadcast(Context context, long conversationId, String title) {
        Intent intent = new Intent(ACTION_UPDATED);
        intent.putExtra(EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(EXTRA_TITLE, title);
        context.sendBroadcast(intent);
        Log.v("conversation_broadcast", "broadcasting new title: " + title);
    }

    /**
     * Sends a broadcast to anywhere that has registered this receiver to let it know to update.
     */
    public static void sendBroadcast(Context context, ConversationUpdateInfo updateInfo) {
        sendBroadcast(context, updateInfo.getConversationId(), updateInfo.getSnippet(), updateInfo.getRead());
        Log.v("conversation_broadcast", "broadcasting new update info: " + updateInfo.getSnippet());
    }

    /**
     * Gets an intent filter that will pick up these broadcasts.
     */
    public static IntentFilter getIntentFilter() {
        return new IntentFilter(ACTION_UPDATED);
    }



    @VisibleForTesting
    protected boolean shouldIgnoreSnippet(String snippet) {
        if (snippet.contains("img.youtube.com")) {
            return true;
        } else if (snippet.contains("{") && snippet.contains("}")) {
            return true;
        } else {
            return false;
        }
    }
}
