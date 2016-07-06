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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.data.Conversation;
import xyz.klinker.messenger.util.AnimationUtil;
import xyz.klinker.messenger.util.ColorUtil;
import xyz.klinker.messenger.util.ConversationExpandedListener;
import xyz.klinker.messenger.util.OnBackPressedListener;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeItemDecoration;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeToDeleteListener;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeTouchHelper;

/**
 * Fragment for displaying the conversation list or an empty screen if there are currently no
 * open conversations.
 */
public class ConversationListFragment extends Fragment
        implements SwipeToDeleteListener, ConversationExpandedListener, OnBackPressedListener {

    private View empty;
    private RecyclerView recyclerView;
    private List<Conversation> pendingDelete;
    private ConversationViewHolder expandedConversation;
    private MessageListFragment messageListFragment;

    public static ConversationListFragment newInstance() {
        return new ConversationListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        View view = inflater.inflate(R.layout.fragment_conversation_list, viewGroup, false);
        empty = view.findViewById(R.id.empty_view);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        loadConversations();

        return view;
    }

    private void loadConversations() {
        setConversations(Conversation.getFakeConversations(getResources()));
    }

    private void setConversations(List<Conversation> conversations) {
        this.pendingDelete = new ArrayList<>();

        if (recyclerView == null) {
            throw new RuntimeException("RecyclerView not yet initialized");
        }

        if (recyclerView.getAdapter() != null) {
            ConversationListAdapter adapter = (ConversationListAdapter) recyclerView.getAdapter();
            adapter.setConversations(conversations);
            adapter.notifyDataSetChanged();
        } else {
            ConversationListAdapter adapter =
                    new ConversationListAdapter(conversations, this, this);

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(new SwipeItemDecoration());

            ItemTouchHelper touchHelper = new SwipeTouchHelper(adapter);
            touchHelper.attachToRecyclerView(recyclerView);
        }

        checkEmptyViewDisplay();
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

        Snackbar.make(recyclerView, plural, Snackbar.LENGTH_LONG)
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

                        if (pendingDelete.size() == currentSize) {
                            // TODO delete pending conversations
                            pendingDelete = new ArrayList<>();
                        }
                    }
                })
                .show();

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
        expandedConversation = viewHolder;
        AnimationUtil.expandActivityForConversation(getActivity());

        messageListFragment = MessageListFragment.newInstance(viewHolder.contact);
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.message_list_container, messageListFragment)
                .commit();
    }

    @Override
    public void onConversationContracted(ConversationViewHolder viewHolder) {
        expandedConversation = null;
        AnimationUtil.contractActivityFromConversation(getActivity());

        getActivity().getSupportFragmentManager().beginTransaction()
                .remove(messageListFragment)
                .commit();
        messageListFragment = null;

        ColorUtil.adjustStatusBarColor(getResources().getColor(R.color.colorPrimaryDark),
                getActivity());
    }

    @Override
    public boolean onBackPressed() {
        if (expandedConversation != null) {
            expandedConversation.itemView.performClick();
            return true;
        } else {
            return false;
        }
    }

}
