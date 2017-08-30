/*
 * Copyright (C) 2016 Jacob Klinker
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

package xyz.klinker.messenger.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.adapter.SearchAdapter;
import xyz.klinker.messenger.shared.MessengerActivityExtras;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.util.listener.SearchListener;

/**
 * A fragment for searching through conversations and messages.
 */
public class SearchFragment extends Fragment implements SearchListener {

    private FragmentActivity activity;

    private RecyclerView list;
    private SearchAdapter adapter;
    private String query;

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        this.activity = getActivity();

        list = (RecyclerView) inflater.inflate(R.layout.fragment_search, parent, false);
        list.setLayoutManager(new LinearLayoutManager(activity));

        if (adapter == null) {
            adapter = new SearchAdapter(query, null, null, this);
        }

        list.setAdapter(adapter);

        return list;
    }

    public void search(String query) {
        this.query = query;
        loadSearch();
    }

    public boolean isSearching() {
        return query != null && query.length() != 0;
    }

    private void loadSearch() {
        final Handler handler = new Handler();

        new Thread(() -> {
            DataSource source = DataSource.INSTANCE;

            final List<Conversation> conversations;
            if (activity != null) {
                conversations = source.searchConversationsAsList(activity, query, 60);
            } else {
                conversations = new ArrayList<>();
            }

            final List<Message> messages;
            if (activity != null) {
                messages = source.searchMessagesAsList(activity, query, 60);
            } else {
                messages = new ArrayList<>();
            }

            handler.post(() -> setSearchResults(conversations, messages));
        }).start();
    }

    private void setSearchResults(List<Conversation> conversations, List<Message> messages) {
        if (adapter != null) {
            adapter.updateCursors(query, conversations, messages);
        } else {
            adapter = new SearchAdapter(query, conversations, messages, this);
        }
    }

    @Override
    public void onSearchSelected(Message message) {
        dismissKeyboard();

        DataSource.INSTANCE.archiveConversation(activity, message.conversationId, false);

        Intent intent = new Intent(activity, MessengerActivity.class);
        intent.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_CONVERSATION_ID(), message.conversationId);
        intent.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_MESSAGE_ID(), message.id);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }

    @Override
    public void onSearchSelected(Conversation conversation) {
        dismissKeyboard();

        if (conversation.archive) {
            DataSource.INSTANCE.archiveConversation(activity, conversation.id, false);
        }

        Intent intent = new Intent(activity, MessengerActivity.class);
        intent.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_CONVERSATION_ID(), conversation.id);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }

    private void dismissKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(list.getWindowToken(), 0);
    }

}
