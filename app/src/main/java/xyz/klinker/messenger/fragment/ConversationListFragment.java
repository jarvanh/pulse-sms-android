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

package xyz.klinker.messenger.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.shared.MessengerActivityExtras;
import xyz.klinker.messenger.MessengerApplication;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.adapter.FixedScrollLinearLayoutManager;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.SectionType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.data.pojo.ConversationUpdateInfo;
import xyz.klinker.messenger.shared.receiver.ConversationListUpdatedReceiver;
import xyz.klinker.messenger.shared.shared_interfaces.IConversationListFragment;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.AnimationUtils;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.SmsMmsUtils;
import xyz.klinker.messenger.shared.util.SnackbarAnimationFix;
import xyz.klinker.messenger.shared.util.TimeUtils;
import xyz.klinker.messenger.shared.util.listener.BackPressedListener;
import xyz.klinker.messenger.utils.listener.ConversationExpandedListener;
import xyz.klinker.messenger.utils.multi_select.ConversationsMultiSelectDelegate;
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeItemDecoration;
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeToDeleteListener;
import xyz.klinker.messenger.utils.swipe_to_dismiss.SwipeTouchHelper;

/**
 * Fragment for displaying the conversation list or an empty screen if there are currently no
 * open conversations.
 */
public class ConversationListFragment extends Fragment
        implements SwipeToDeleteListener, ConversationExpandedListener, BackPressedListener, IConversationListFragment {

    private static final String ARG_CONVERSATION_TO_OPEN_ID = "conversation_to_open";
    private static final String ARG_MESSAGE_TO_OPEN_ID = "message_to_open";

    private long lastRefreshTime = 0;
    private View empty;
    private FixedScrollLinearLayoutManager layoutManager;
    private RecyclerView recyclerView;
    private List<Conversation> pendingDelete = new ArrayList<>();
    protected List<Conversation> pendingArchive = new ArrayList<>();
    private ConversationViewHolder expandedConversation;
    private MessageListFragment messageListFragment;
    private Snackbar deleteSnackbar;
    public  Snackbar archiveSnackbar;
    private ConversationListAdapter adapter;
    private ConversationListUpdatedReceiver updatedReceiver;
    private ConversationsMultiSelectDelegate multiSelector;

    private String newConversationTitle = null;
    public ConversationUpdateInfo updateInfo = null;

    protected FragmentActivity activity;

    public static ConversationListFragment newInstance() {
        return newInstance(-1);
    }

    public static ConversationListFragment newInstance(long conversationToOpenId) {
        return newInstance(conversationToOpenId, -1);
    }

    public static ConversationListFragment newInstance(long conversationToOpenId,
                                                       long messageToOpenId) {
        ConversationListFragment fragment = new ConversationListFragment();
        Bundle bundle = new Bundle();

        if (conversationToOpenId != -1) {
            bundle.putLong(ARG_CONVERSATION_TO_OPEN_ID, conversationToOpenId);
        }

        if (messageToOpenId != -1) {
            bundle.putLong(ARG_MESSAGE_TO_OPEN_ID, messageToOpenId);
        }

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup viewGroup, Bundle bundle) {
        this.activity = getActivity();

        View view = inflater.inflate(R.layout.fragment_conversation_list, viewGroup, false);
        empty = view.findViewById(R.id.empty_view);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        loadConversations();

        empty.setBackgroundColor(Settings.INSTANCE.getMainColorSet().getColorLight());
        ColorUtils.INSTANCE.changeRecyclerOverscrollColors(recyclerView, Settings.INSTANCE.getMainColorSet().getColor());

        multiSelector = new ConversationsMultiSelectDelegate(this);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updatedReceiver = new ConversationListUpdatedReceiver(this);
        activity.registerReceiver(updatedReceiver,
                ConversationListUpdatedReceiver.Companion.getIntentFilter());
    }

    @Override
    public void onStart() {
        super.onStart();

        // if the refresh time was more than an hour ago and there isn't an expanded conversation,
        // refresh the list
        if (System.currentTimeMillis() - lastRefreshTime > 1000 * 60 * 60 &&
                expandedConversation == null) {
            loadConversations();
        }

        checkUnreadCount();

        if (messageListFragment != null && !messageListFragment.isAdded()) {
            Intent main = new Intent(activity, MessengerActivity.class);
            main.putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_CONVERSATION_ID(),
                    messageListFragment.getConversationId());

            activity.overridePendingTransition(0,0);
            activity.finish();

            activity.overridePendingTransition(0,0);
            activity.startActivity(main);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (updatedReceiver != null) {
            activity.unregisterReceiver(updatedReceiver);
        }

        multiSelector.clearActionMode();
    }

    public void makeSnackbar(String text, int duration, String actionLabel, View.OnClickListener listener) {
        Snackbar s = Snackbar.make(recyclerView, text, duration);

        if (actionLabel != null && listener != null) {
            s.setAction(actionLabel, listener);
        }

        SnackbarAnimationFix.INSTANCE.apply(s);
        s.show();
    }

    public void dismissSnackbars(Activity activity) {
        if (activity == null) {
            return;
        }

        if (archiveSnackbar != null && archiveSnackbar.isShown()) {
            archiveSnackbar.dismiss();

            // force them to be deleted. Activity will be null from getActivity, so we don't rely on the delete snackbar's callback
            dismissArchiveSnackbar(activity, -1);
        }

        if (deleteSnackbar != null && deleteSnackbar.isShown()) {
            deleteSnackbar.dismiss();

            // force them to be deleted. Activity will be null from getActivity, so we don't rely on the delete snackbar's callback
            dismissDeleteSnackbar(activity, -1);
        }
    }

    protected List<Conversation> getCursor(DataSource source) {
        if (activity != null) {
            return source.getUnarchivedConversationsAsList(activity);
        } else {
            return new ArrayList<>();
        }
    }

    public void loadConversations() {
        final Handler handler = new Handler();
        new Thread(() -> {
            long startTime = System.currentTimeMillis();

            if (activity == null) {
                return;
            }
            
            final List<Conversation> conversations = getCursor(DataSource.INSTANCE);

            Log.v("conversation_load", "load took " + (
                    System.currentTimeMillis() - startTime) + " ms");

            if (activity == null) {
                return;
            }

            handler.post(() -> {
                setConversations(conversations);
                lastRefreshTime = System.currentTimeMillis();

                try {
                    ((MessengerApplication) activity.getApplication()).refreshDynamicShortcuts();
                } catch (Exception e) { }
            });
        }).start();
    }

    public ItemTouchHelper getSwipeTouchHelper(ConversationListAdapter adapter) {
        return new SwipeTouchHelper(adapter, activity);
    }

    private void setConversations(List<Conversation> conversations) {
        this.pendingDelete = new ArrayList<>();
        this.pendingArchive = new ArrayList<>();

        if (recyclerView == null) {
            throw new RuntimeException("RecyclerView not yet initialized");
        }

        adapter = (ConversationListAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.setConversations(conversations);
            adapter.notifyDataSetChanged();
        } else {
            adapter = new ConversationListAdapter((MessengerActivity) activity,
                    conversations, multiSelector, this, this);

            layoutManager = new FixedScrollLinearLayoutManager(activity);
            layoutManager.setCanScroll(true);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(new SwipeItemDecoration());

            ItemTouchHelper touchHelper = getSwipeTouchHelper(adapter);
            touchHelper.attachToRecyclerView(recyclerView);
        }

        if (getArguments() != null) {
            long conversationToOpen = getArguments().getLong(ARG_CONVERSATION_TO_OPEN_ID, -1L);
            getArguments().putLong(ARG_CONVERSATION_TO_OPEN_ID, -1L);

            if (conversationToOpen != -1L) {
                clickConversationWithId(conversationToOpen);
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

    private Handler clickHandler;
    private void clickConversationAtPosition(final int position) {
        if (clickHandler == null) {
            clickHandler = new Handler();
        } else {
            clickHandler.removeCallbacksAndMessages(null);
        }

        clickHandler.postDelayed(() -> {
            try {
                View itemView = recyclerView.findViewHolderForAdapterPosition(position)
                        .itemView;
                itemView.setSoundEffectsEnabled(false);
                itemView.performClick();
                itemView.setSoundEffectsEnabled(true);
            } catch (Exception e) {
                // not yet ready to click
                clickConversationAtPosition(position);
            }
        }, 100);
    }

    public void checkEmptyViewDisplay() {
        if (recyclerView.getAdapter().getItemCount() == 0 && empty.getVisibility() == View.GONE) {
            empty.setAlpha(0);
            empty.setVisibility(View.VISIBLE);

            empty.animate().alpha(1f).setDuration(250).setListener(null);
        } else if (recyclerView.getAdapter().getItemCount() != 0 &&
                empty.getVisibility() == View.VISIBLE) {
            empty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSwipeToDelete(final Conversation conversation) {
        pendingDelete.add(conversation);
        final int currentSize = pendingDelete.size();

        if (isDetached()) {
            dismissDeleteSnackbar(activity, currentSize);
            return;
        }

        String plural = getResources().getQuantityString(R.plurals.conversations_deleted,
                pendingDelete.size(), pendingDelete.size());

        if (archiveSnackbar != null && archiveSnackbar.isShown()) {
            archiveSnackbar.dismiss();
        }

        deleteSnackbar = Snackbar.make(recyclerView, plural, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, view -> {
                    pendingDelete = new ArrayList<>();
                    loadConversations();
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        dismissDeleteSnackbar(activity, currentSize);
                    }
                });
        SnackbarAnimationFix.INSTANCE.apply(deleteSnackbar);
        deleteSnackbar.show();

        if (conversation != null) {
            NotificationManagerCompat.from(activity).cancel((int) conversation.getId());
        }

        // for some reason, if this is done immediately then the final snackbar will not be
        // displayed
        new Handler().postDelayed(this::checkEmptyViewDisplay, 500);
    }

    private void dismissDeleteSnackbar(final Activity activity, final int currentSize) {
        new Thread(() -> {
            if ((currentSize == -1 || pendingDelete.size() == currentSize) && activity != null) {
                DataSource dataSource = DataSource.INSTANCE;

                // we were getting a concurrent modification exception, so
                // copy this list and continue to delete in the background.
                List<Conversation> copiedList = new ArrayList<>(pendingDelete);
                for (Conversation conversation : copiedList) {
                    if (conversation != null && activity != null) { // there are those blank convos that get populated with a new one
                        dataSource.deleteConversation(activity, conversation);
                        SmsMmsUtils.INSTANCE.deleteConversation(activity, conversation.getPhoneNumbers());
                    }
                }

                copiedList.clear();
                pendingDelete = new ArrayList<>();
            }
        }).start();
    }

    protected String getArchiveSnackbarText() {
        return getResources().getQuantityString(R.plurals.conversations_archived,
                pendingArchive.size(), pendingArchive.size());
    }

    protected void performArchiveOperation(DataSource dataSource, Conversation conversation) {
        if (activity != null) {
            dataSource.archiveConversation(activity, conversation.getId());
        }
    }

    @Override
    public void onSwipeToArchive(final Conversation conversation) {
        pendingArchive.add(conversation);
        final int currentSize = pendingArchive.size();

        if (!isAdded()) {
            dismissArchiveSnackbar(activity, currentSize);
            return;
        }

        if (deleteSnackbar != null && deleteSnackbar.isShown()) {
            deleteSnackbar.dismiss();
        }

        String plural = getArchiveSnackbarText();

        archiveSnackbar = Snackbar.make(recyclerView, plural, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, view -> {
                    pendingArchive = new ArrayList<>();
                    loadConversations();
                })
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        dismissArchiveSnackbar(activity, currentSize);
                    }
                });
        SnackbarAnimationFix.INSTANCE.apply(archiveSnackbar);
        archiveSnackbar.show();

        NotificationManagerCompat.from(activity).cancel((int) conversation.getId());

        // for some reason, if this is done immediately then the final snackbar will not be
        // displayed
        new Handler().postDelayed(this::checkEmptyViewDisplay, 500);
    }

    private void dismissArchiveSnackbar(final Activity activity, final int currentSize) {
        new Thread(() -> {
            if ((currentSize == -1 || pendingArchive.size() == currentSize) && activity != null) {
                // we were getting a concurrent modification exception, so
                // copy this list and continue to archive in the background.
                List<Conversation> copiedList = new ArrayList<>(pendingArchive);
                for (Conversation conversation : copiedList) {
                    if (conversation != null) {
                        performArchiveOperation(DataSource.INSTANCE, conversation);
                    }
                }

                copiedList.clear();
                pendingArchive = new ArrayList<>();
            }
        }).start();
    }

    @Override
    public void onShowMarkAsRead(String text) {
        Toast.makeText(activity, getString(R.string.mark_section_as_read, text.toLowerCase(Locale.US)), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMarkSectionAsRead(String text, final int sectionType) {
        Snackbar snackbar = Snackbar.make(recyclerView, getString(R.string.marking_section_as_read, text.toLowerCase(Locale.US)), Snackbar.LENGTH_LONG);
        SnackbarAnimationFix.INSTANCE.apply(snackbar);
        snackbar.show();

        final List<Conversation> allConversations = adapter.getConversations();
        final List<Conversation> markAsRead = new ArrayList<>();

        final Handler handler = new Handler();
        new Thread(() -> {
            for (Conversation conversation : allConversations) {
                boolean shouldRead = false;
                if (sectionType == SectionType.Companion.getPINNED()) {
                    shouldRead = conversation.getPinned();
                } else if (sectionType == SectionType.Companion.getTODAY()) {
                    shouldRead = TimeUtils.INSTANCE.isToday(conversation.getTimestamp());
                } else if (sectionType == SectionType.Companion.getYESTERDAY()) {
                    shouldRead = TimeUtils.INSTANCE.isYesterday(conversation.getTimestamp());
                } else if (sectionType == SectionType.Companion.getLAST_WEEK()) {
                    shouldRead = TimeUtils.INSTANCE.isLastWeek(conversation.getTimestamp());
                } else if (sectionType == SectionType.Companion.getLAST_MONTH()) {
                    shouldRead = TimeUtils.INSTANCE.isLastMonth(conversation.getTimestamp());
                }

                if (shouldRead) {
                    markAsRead.add(conversation);
                }
            }

            DataSource.INSTANCE.readConversations(activity, markAsRead);
            handler.post(this::loadConversations);
        }).start();
    }

    @Override
    public boolean onConversationExpanded(ConversationViewHolder viewHolder) {
        updateInfo = null;

        if (expandedConversation != null) {
            return false;
        }

        if (deleteSnackbar != null && deleteSnackbar.isShown()) {
            deleteSnackbar.dismiss();
        }

        expandedConversation = viewHolder;
        AnimationUtils.INSTANCE.expandActivityForConversation(activity);

        if (getArguments() != null && getArguments().containsKey(ARG_MESSAGE_TO_OPEN_ID)) {
            messageListFragment = MessageListFragment.newInstance(viewHolder.conversation,
                    getArguments().getLong(ARG_MESSAGE_TO_OPEN_ID));
            getArguments().remove(ARG_MESSAGE_TO_OPEN_ID);
        } else {
            messageListFragment = MessageListFragment.newInstance(viewHolder.conversation);
        }

        try {
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.message_list_container, messageListFragment)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!Settings.INSTANCE.getUseGlobalThemeColor()) {
            ActivityUtils.INSTANCE.setTaskDescription(activity,
                    viewHolder.conversation.getTitle(), viewHolder.conversation.getColors().getColor());
        }
        
        checkUnreadCount();
        layoutManager.setCanScroll(false);

        if (activity != null)
            activity.getIntent().putExtra(MessengerActivityExtras.INSTANCE.getEXTRA_CONVERSATION_ID(), -1L);

        if (getArguments() != null)
            getArguments().putLong(ARG_CONVERSATION_TO_OPEN_ID, -1L);
        
        return true;
    }

    @Override
    public void onConversationContracted(ConversationViewHolder viewHolder) {
        expandedConversation = null;
        AnimationUtils.INSTANCE.contractActivityFromConversation(activity);

        if (messageListFragment == null) {
            return;
        }

        long contractedId = messageListFragment.getConversationId();

        try {
            new Handler().postDelayed(() -> {
                if (activity != null) {
                    try {
                        activity.getSupportFragmentManager().beginTransaction()
                                .remove(messageListFragment)
                                .commit();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                messageListFragment = null;
            }, AnimationUtils.INSTANCE.getEXPAND_CONVERSATION_DURATION());

            messageListFragment.getView().animate().alpha(0f).setDuration(100).start();
        } catch (Exception e) {

        }

        ColorSet color = Settings.INSTANCE.getMainColorSet();
        ColorUtils.INSTANCE.adjustStatusBarColor(color.getColorDark(), activity);
        ColorUtils.INSTANCE.adjustDrawerColor(color.getColorDark(), activity);

        ColorUtils.INSTANCE.changeRecyclerOverscrollColors(recyclerView, color.getColor());
        ActivityUtils.INSTANCE.setTaskDescription(activity);

        if (updateInfo != null) {
            ConversationListUpdatedReceiver.Companion.sendBroadcast(activity, updateInfo);
            updateInfo = null;
        }

        if (newConversationTitle != null) {
            ConversationListUpdatedReceiver.Companion.sendBroadcast(activity, contractedId, newConversationTitle);
            newConversationTitle = null;
        }

        checkUnreadCount();
        layoutManager.setCanScroll(true);
    }

    @Override
    public boolean onBackPressed() {
        if (messageListFragment != null && messageListFragment.onBackPressed()) {
            return true;
        } else if (expandedConversation != null) {
            View conversation = expandedConversation.itemView;
            conversation.setSoundEffectsEnabled(false);
            conversation.performClick();
            conversation.setSoundEffectsEnabled(true);
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
            return expandedConversation.conversation.getId();
        } else {
            return 0;
        }
    }

    public ConversationViewHolder getExpandedItem() {
        return expandedConversation;
    }

    public void notifyOfSentMessage(Message m) {
        if (m == null) {
            return;
        }

        expandedConversation.conversation.setTimestamp(m.getTimestamp());
        expandedConversation.conversation.setRead(m.getRead());

        if (m.getMimeType() != null && m.getMimeType().equals("text/plain")) {
            expandedConversation.conversation.setSnippet(m.getData());
            expandedConversation.summary.setText(m.getData());
        }

        setConversationUpdateInfo(new ConversationUpdateInfo(
                expandedConversation.conversation.getId(), getString(R.string.you) + ": " + m.getData(), true));
    }

    public ConversationListAdapter getAdapter() {
        return adapter;
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void setConversationUpdateInfo(ConversationUpdateInfo info) {
        this.updateInfo = info;
    }

    public void setNewConversationTitle(String title) {
        this.newConversationTitle = title;
    }

    private void checkUnreadCount() {
        // This should be managed by clearing the count when the is started instead.
//        new Thread(() -> {
//            try {
//                Thread.sleep(1500);
//            } catch (InterruptedException e) { }
//
//            new UnreadBadger(activity).writeCountFromDatabase();
//        }).start();
    }
}
