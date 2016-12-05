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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
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

import xyz.klinker.messenger.MessengerApplication;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.data.SectionType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.receiver.ConversationListUpdatedReceiver;
import xyz.klinker.messenger.service.NotificationService;
import xyz.klinker.messenger.util.ActivityUtils;
import xyz.klinker.messenger.util.AnimationUtils;
import xyz.klinker.messenger.util.ColorUtils;
import xyz.klinker.messenger.util.SmsMmsUtils;
import xyz.klinker.messenger.util.TimeUtils;
import xyz.klinker.messenger.util.UnreadBadger;
import xyz.klinker.messenger.util.UpdateUtils;
import xyz.klinker.messenger.util.listener.BackPressedListener;
import xyz.klinker.messenger.util.listener.ConversationExpandedListener;
import xyz.klinker.messenger.util.multi_select.ConversationsMultiSelectDelegate;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeItemDecoration;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeToDeleteListener;
import xyz.klinker.messenger.util.swipe_to_dismiss.SwipeTouchHelper;

/**
 * Fragment for displaying the conversation list or an empty screen if there are currently no
 * open conversations.
 */
public class ConversationListFragment extends Fragment
        implements SwipeToDeleteListener, ConversationExpandedListener, BackPressedListener {

    private static final String ARG_CONVERSATION_TO_OPEN_ID = "conversation_to_open";
    private static final String ARG_MESSAGE_TO_OPEN_ID = "message_to_open";

    private long lastRefreshTime = 0;
    private View empty;
    private RecyclerView recyclerView;
    private List<Conversation> pendingDelete;
    protected List<Conversation> pendingArchive;
    private ConversationViewHolder expandedConversation;
    private MessageListFragment messageListFragment;
    private Snackbar deleteSnackbar;
    public  Snackbar archiveSnackbar;
    private ConversationListAdapter adapter;
    private ConversationListUpdatedReceiver updatedReceiver;
    private ConversationsMultiSelectDelegate multiSelector;

    private String newConversationTitle = null;
    public ConversationListUpdatedReceiver.ConversationUpdateInfo updateInfo = null;

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
        View view = inflater.inflate(R.layout.fragment_conversation_list, viewGroup, false);
        empty = view.findViewById(R.id.empty_view);
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        loadConversations();

        Settings settings = Settings.get(getActivity());
        if (settings.useGlobalThemeColor) {
            empty.setBackgroundColor(settings.globalColorSet.colorLight);
            ColorUtils.changeRecyclerOverscrollColors(recyclerView, settings.globalColorSet.color);
        }

        multiSelector = new ConversationsMultiSelectDelegate(this);

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
        if (System.currentTimeMillis() - lastRefreshTime > 1000 * 60 * 60 &&
                expandedConversation == null) {
            loadConversations();
        }

        checkUnreadCount();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismissSnackbars(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (updatedReceiver != null) {
            getActivity().unregisterReceiver(updatedReceiver);
        }

        multiSelector.clearActionMode();
    }

    public void makeSnackbar(String text, int duration, String actionLabel, View.OnClickListener listener) {
        Snackbar s = Snackbar.make(recyclerView, text, duration);

        if (actionLabel != null && listener != null) {
            s.setAction(actionLabel, listener);
        }

        s.show();
    }

    public void dismissSnackbars(Activity activity) {
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
        return source.getUnarchivedConversationsAsList();
    }

    public void loadConversations() {
        final Handler handler = new Handler();
        new Thread(() -> {
            if (getActivity() == null) {
                return;
            }

            long startTime = System.currentTimeMillis();
            final DataSource source = DataSource.getInstance(getActivity());
            source.open();
            final List<Conversation> conversations = getCursor(source);
            source.close();

            Log.v("conversation_load", "load took " + (
                    System.currentTimeMillis() - startTime) + " ms");

            handler.post(() -> {
                setConversations(conversations);
                lastRefreshTime = System.currentTimeMillis();

                try {
                    ((MessengerApplication) getActivity().getApplication()).refreshDynamicShortcuts();
                } catch (Exception e) { }
            });
        }).start();
    }

    public ItemTouchHelper getSwipeTouchHelper(ConversationListAdapter adapter) {
        return new SwipeTouchHelper(adapter);
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
            adapter = new ConversationListAdapter(conversations, multiSelector, this, this);

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(new SwipeItemDecoration());

            ItemTouchHelper touchHelper = getSwipeTouchHelper(adapter);
            touchHelper.attachToRecyclerView(recyclerView);
        }

        if (getArguments() != null) {
            long conversationToOpen = getArguments().getLong(ARG_CONVERSATION_TO_OPEN_ID, 0);
            if (conversationToOpen != 0) {
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
                recyclerView.findViewHolderForAdapterPosition(position)
                        .itemView.performClick();
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
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        dismissDeleteSnackbar(getActivity(), currentSize);
                    }
                });
        deleteSnackbar.show();

        // for some reason, if this is done immediately then the final snackbar will not be
        // displayed
        new Handler().postDelayed(() -> checkEmptyViewDisplay(), 500);
    }

    private void dismissDeleteSnackbar(final Activity activity, final int currentSize) {
        new Thread(() -> {
            if ((currentSize == -1 || pendingDelete.size() == currentSize) && activity != null) {
                DataSource dataSource = DataSource.getInstance(activity);
                dataSource.open();

                // we were getting a concurrent modification exception, so
                // copy this list and continue to delete in the background.
                List<Conversation> copiedList = new ArrayList<>(pendingDelete);
                for (Conversation conversation : copiedList) {
                    if (conversation != null) { // there are those blank convos that get populated with a new one
                        dataSource.deleteConversation(conversation);
                        SmsMmsUtils.deleteConversation(getContext(),
                                conversation.phoneNumbers);
                    }
                }

                dataSource.close();
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
        dataSource.archiveConversation(conversation.id);
    }

    @Override
    public void onSwipeToArchive(final Conversation conversation) {
        pendingArchive.add(conversation);
        final int currentSize = pendingArchive.size();

        if (deleteSnackbar != null && deleteSnackbar.isShown()) {
            deleteSnackbar.dismiss();
        }

        String plural = getArchiveSnackbarText();

        archiveSnackbar = Snackbar.make(recyclerView, plural, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, view -> {
                    pendingArchive = new ArrayList<>();
                    loadConversations();
                })
                .setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        dismissArchiveSnackbar(getActivity(), currentSize);
                    }
                });
        archiveSnackbar.show();

        // for some reason, if this is done immediately then the final snackbar will not be
        // displayed
        new Handler().postDelayed(() -> checkEmptyViewDisplay(), 500);
    }

    private void dismissArchiveSnackbar(final Activity activity, final int currentSize) {
        new Thread(() -> {
            if ((currentSize == -1 || pendingArchive.size() == currentSize) && activity != null) {
                DataSource dataSource = DataSource.getInstance(activity);
                dataSource.open();

                // we were getting a concurrent modification exception, so
                // copy this list and continue to archive in the background.
                List<Conversation> copiedList = new ArrayList<>(pendingArchive);
                for (Conversation conversation : copiedList) {
                    if (conversation != null) {
                        performArchiveOperation(dataSource, conversation);
                    }
                }

                dataSource.close();
                copiedList.clear();
                pendingArchive = new ArrayList<>();
            }
        }).start();
    }

    @Override
    public void onShowMarkAsRead(String text) {
        Toast.makeText(getActivity(), getString(R.string.mark_section_as_read, text.toLowerCase(Locale.US)), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMarkSectionAsRead(String text, final int sectionType) {
        Snackbar.make(recyclerView, getString(R.string.marking_section_as_read, text.toLowerCase(Locale.US)), Snackbar.LENGTH_LONG).show();

        final List<Conversation> allConversations = adapter.getConversations();
        final List<Conversation> markAsRead = new ArrayList<>();

        final Handler handler = new Handler();
        new Thread(() -> {
            for (Conversation conversation : allConversations) {
                boolean shouldRead = false;
                switch (sectionType) {
                    case SectionType.PINNED:
                        shouldRead = conversation.pinned;
                        break;
                    case SectionType.TODAY:
                        shouldRead = TimeUtils.isToday(conversation.timestamp);
                        break;
                    case SectionType.YESTERDAY:
                        shouldRead = TimeUtils.isYesterday(conversation.timestamp);
                        break;
                    case SectionType.LAST_WEEK:
                        shouldRead = TimeUtils.isLastWeek(conversation.timestamp);
                        break;
                    case SectionType.LAST_MONTH:
                        shouldRead = TimeUtils.isLastMonth(conversation.timestamp);
                        break;
                    default:
                        shouldRead = true;
                        break;
                }

                if (shouldRead) {
                    markAsRead.add(conversation);
                }
            }

            DataSource source = DataSource.getInstance(getActivity());
            source.open();
            for (Conversation conversation : markAsRead) {
                source.readConversation(getActivity(), conversation.id);
            }
            source.close();

            handler.post(() -> loadConversations());
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

        if (viewHolder.conversation != null) {
            NotificationService.CONVERSATION_ID_OPEN = viewHolder.conversation.id;
        }

        expandedConversation = viewHolder;
        AnimationUtils.expandActivityForConversation(getActivity());

        if (getArguments() != null && getArguments().containsKey(ARG_MESSAGE_TO_OPEN_ID)) {
            messageListFragment = MessageListFragment.newInstance(viewHolder.conversation,
                    getArguments().getLong(ARG_MESSAGE_TO_OPEN_ID));
        } else {
            messageListFragment = MessageListFragment.newInstance(viewHolder.conversation);
        }

        if (getActivity() != null)
                getActivity().getIntent().putExtra(MessengerActivity.EXTRA_CONVERSATION_ID, -1L);
        if (getArguments() != null)
                getArguments().putLong(ARG_CONVERSATION_TO_OPEN_ID, -1L);

        try {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.message_list_container, messageListFragment)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!Settings.get(getActivity()).useGlobalThemeColor) {
            ActivityUtils.setTaskDescription(getActivity(),
                    viewHolder.conversation.title, viewHolder.conversation.colors.color);
        }
        
        checkUnreadCount();
        return true;
    }

    @Override
    public void onConversationContracted(ConversationViewHolder viewHolder) {
        expandedConversation = null;
        AnimationUtils.contractActivityFromConversation(getActivity());

        long contractedId = messageListFragment.getConversationId();

        try {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .remove(messageListFragment)
                    .commit();
        } catch (Exception e) {

        }

        messageListFragment = null;

        int color = getResources().getColor(R.color.colorPrimaryDark);
        ColorUtils.adjustStatusBarColor(color, getActivity());
        ColorUtils.adjustDrawerColor(color, getActivity());

        if (!Settings.get(getActivity()).useGlobalThemeColor) {
            ActivityUtils.setTaskDescription(getActivity());
        }

        if (viewHolder.conversation != null) {
            NotificationService.CONVERSATION_ID_OPEN = 0L;
        }

        if (updateInfo != null) {
            ConversationListUpdatedReceiver.sendBroadcast(getActivity(), updateInfo);
            updateInfo = null;
        }

        if (newConversationTitle != null) {
            ConversationListUpdatedReceiver.sendBroadcast(getActivity(), contractedId, newConversationTitle);
            newConversationTitle = null;
        }
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

        setConversationUpdateInfo(new ConversationListUpdatedReceiver.ConversationUpdateInfo(
                expandedConversation.conversation.id, getString(R.string.you) + ": " + m.data, true));
    }

    public ConversationListAdapter getAdapter() {
        return adapter;
    }

    public void setConversationUpdateInfo(ConversationListUpdatedReceiver.ConversationUpdateInfo info) {
        this.updateInfo = info;
    }

    public void setNewConversationTitle(String title) {
        this.newConversationTitle = title;
    }

    private void checkUnreadCount() {
        new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) { }

            new UnreadBadger(getActivity()).writeCountFromDatabase();
        }).start();
    }
}
