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

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.receiver.ConversationListUpdatedReceiver;
import xyz.klinker.messenger.util.AnimationUtils;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.listener.ConversationExpandedListener;
import xyz.klinker.messenger.util.listener.BackPressedListener;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeItemDecoration;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeToDeleteListener;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeTouchHelper;

/**
 * Fragment for displaying the conversation list or an empty screen if there are currently no
 * open conversations.
 */
public class ConversationListFragment extends Fragment
        implements SwipeToDeleteListener, ConversationExpandedListener, BackPressedListener {

    private static final String EXTRA_CONVERSATION_TO_OPEN_ID = "conversation_to_open";

    private long lastRefreshTime = 0;
    private View empty;
    private RecyclerView recyclerView;
    private List<Conversation> pendingDelete;
    private ConversationViewHolder expandedConversation;
    private MessageListFragment messageListFragment;
    private Snackbar deleteSnackbar;
    private ConversationListAdapter adapter;
    private ConversationListUpdatedReceiver updatedReceiver;

    public static ConversationListFragment newInstance() {
        return new ConversationListFragment();
    }

    public static ConversationListFragment newInstance(long conversationToOpenId) {
        ConversationListFragment fragment = new ConversationListFragment();
        Bundle bundle = new Bundle();
        bundle.putLong(EXTRA_CONVERSATION_TO_OPEN_ID, conversationToOpenId);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_conversation_list, viewGroup, false);
        empty = view.findViewById(R.id.empty_view);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        loadConversations();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatedReceiver = new ConversationListUpdatedReceiver(this);
        getActivity().registerReceiver(updatedReceiver,
                ConversationListUpdatedReceiver.getIntentFilter());
    }

    @Override
    public void onStart() {
        super.onStart();

        // if the refresh time was more than an hour ago and there isn't an expanded conversation,
        // refresh the list
        if (lastRefreshTime > 1000 * 60 * 60 && expandedConversation == null) {
            loadConversations();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (updatedReceiver != null) {
            getActivity().unregisterReceiver(updatedReceiver);
        }
    }

    private void loadConversations() {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                final DataSource source = DataSource.getInstance(getActivity());
                source.open();
                final Cursor conversations = source.getConversations();
                Log.v("conversation_load", "load took " + (
                        System.currentTimeMillis() - startTime) + " ms");

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setConversations(conversations);
                        source.close();
                        lastRefreshTime = System.currentTimeMillis();
                    }
                });
            }
        }).start();
    }

    private void setConversations(Cursor conversations) {
        this.pendingDelete = new ArrayList<>();

        if (recyclerView == null) {
            throw new RuntimeException("RecyclerView not yet initialized");
        }

        adapter = (ConversationListAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.setConversations(conversations);
            adapter.notifyDataSetChanged();
        } else {
            adapter = new ConversationListAdapter(conversations, this, this);

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(new SwipeItemDecoration());

            ItemTouchHelper touchHelper = new SwipeTouchHelper(adapter);
            touchHelper.attachToRecyclerView(recyclerView);
        }

        if (getArguments() != null) {
            long conversationToOpen = getArguments().getLong(EXTRA_CONVERSATION_TO_OPEN_ID, 0);
            if (conversationToOpen != 0) {
                clickConversationWithId(conversationToOpen);
                getArguments().putLong(EXTRA_CONVERSATION_TO_OPEN_ID, 0);
            }
        } else {
            Log.v("Conversation List", "no conversations to open");
        }

        checkEmptyViewDisplay();
    }

    private void clickConversationWithId(long id) {
        final int conversationPosition = adapter.findPositionForConversationId(id);

        if (conversationPosition != -1) {
            recyclerView.getLayoutManager().scrollToPosition(conversationPosition);
            clickConversationAtPosition(conversationPosition);
        }
    }

    private void clickConversationAtPosition(final int position) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    recyclerView.findViewHolderForAdapterPosition(position)
                            .itemView.performClick();
                } catch (Exception e) {
                    // not yet ready to click
                    clickConversationAtPosition(position);
                }
            }
        }, 100);
    }

    private void checkEmptyViewDisplay() {
        if (recyclerView.getAdapter().getItemCount() == 0 &&
                empty.getVisibility() == View.GONE) {
            empty.setAlpha(0);
            empty.setVisibility(View.VISIBLE);

            empty.animate().alpha(1f).setDuration(250).setListener(null);
        } else if (empty.getVisibility() == View.VISIBLE) {
            empty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSwipeToDelete(final Conversation conversation) {
        pendingDelete.add(conversation);
        final int currentSize = pendingDelete.size();

        String plural = getResources().getQuantityString(R.plurals.conversations_deleted,
                pendingDelete.size(), pendingDelete.size());

        deleteSnackbar = Snackbar.make(recyclerView, plural, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        loadConversations();
                    }
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        deleteSnackbar = null;

                        if (pendingDelete.size() == currentSize) {
                            DataSource dataSource = DataSource.getInstance(getActivity());
                            dataSource.open();

                            for (Conversation conversation : pendingDelete) {
                                dataSource.deleteConversation(conversation);
                            }

                            dataSource.close();
                            pendingDelete = new ArrayList<>();
                        }
                    }
                });
        deleteSnackbar.show();

        // for some reason, if this is done immediately then the final snackbar will not be
        // displayed
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkEmptyViewDisplay();
            }
        }, 500);
    }

    @Override
    public void onConversationExpanded(ConversationViewHolder viewHolder) {
    	if (deleteSnackbar != null && deleteSnackbar.isShown()) {
    		deleteSnackbar.dismiss();
    	}
    	
        expandedConversation = viewHolder;
        AnimationUtils.expandActivityForConversation(getActivity());

        messageListFragment = MessageListFragment.newInstance(viewHolder.conversation);
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.message_list_container, messageListFragment)
                .commit();
    }

    @Override
    public void onConversationContracted(ConversationViewHolder viewHolder) {
        expandedConversation = null;
        AnimationUtils.contractActivityFromConversation(getActivity());

        getActivity().getSupportFragmentManager().beginTransaction()
                .remove(messageListFragment)
                .commit();
        messageListFragment = null;

        int color = getResources().getColor(R.color.colorPrimaryDark);
        ColorUtils.adjustStatusBarColor(color, getActivity());
        ColorUtils.adjustDrawerColor(color, getActivity());
    }

    @Override
    public boolean onBackPressed() {
        if (messageListFragment != null && messageListFragment.onBackPressed()) {
            return true;
        } else if (expandedConversation != null) {
            expandedConversation.itemView.performClick();
            return true;
        } else {
            return false;
        }
    }

    public boolean isExpanded() {
        return expandedConversation != null;
    }

    public long getExpandedId() {
        if (expandedConversation != null) {
            return expandedConversation.conversation.id;
        } else {
            return 0;
        }
    }

    public ConversationViewHolder getExpandedItem() {
        return expandedConversation;
    }

    public void notifyOfSentMessage(Message m) {
        expandedConversation.conversation.timestamp = m.timestamp;
        expandedConversation.conversation.read = m.read;

        if (m.mimeType.equals("text/plain")) {
            expandedConversation.conversation.snippet = m.data;
            expandedConversation.summary.setText(m.data);
        }
    }

    public ConversationListAdapter getAdapter() {
        return adapter;
    }

}
