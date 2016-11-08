package xyz.klinker.messenger.util.multi_select;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.fragment.ArchivedConversationListFragment;
import xyz.klinker.messenger.fragment.ConversationListFragment;

public class ConversationsMultiSelectDelegate extends MultiSelector {

    private AppCompatActivity activity;
    private ConversationListFragment fragment;
    private ConversationListAdapter adapter;

    private ActionMode mode;
    private ActionMode.Callback actionMode = new ModalMultiSelectorCallback(this) {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            super.onCreateActionMode(actionMode, menu);

            if (fragment instanceof ArchivedConversationListFragment) {
                activity.getMenuInflater().inflate(R.menu.action_mode_archive_list, menu);
            } else {
                activity.getMenuInflater().inflate(R.menu.action_mode_conversation_list, menu);
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            clearSelections();

            // https://github.com/bignerdranch/recyclerview-multiselect/issues/9#issuecomment-140180348
            try {
                Field field = ConversationsMultiSelectDelegate.this.getClass().getDeclaredField("mIsSelectable");
                if (field != null) {
                    if (!field.isAccessible())
                        field.setAccessible(true);
                    field.set(this, false);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setSelectable(false);
                }
            }, 250);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean handled = false;

            List<Integer> selectedPositions = new ArrayList<>();
            List<Conversation> selectedConversations = new ArrayList<>();
            for (int i = 0; i < adapter.getItemCount(); i++) {
                if (isSelected(i, 0)) {
                    selectedPositions.add(Integer.valueOf(i));
                    selectedConversations.add(adapter.findConversationForPosition(i));
                }
            }

            DataSource source = DataSource.getInstance(activity);

            switch (item.getItemId()) {
                case R.id.menu_archive_conversation:
                    handled = true;

                    int removed = 0;
                    for (Integer i : selectedPositions) {
                        adapter.archiveItem(i - removed);
                        removed++;
                    }

                    break;
                case R.id.menu_delete_conversation:
                    handled = true;

                    removed = 0;
                    for (Integer i : selectedPositions) {
                        adapter.deleteItem(i - removed);
                        removed++;
                    }

                    break;
                case R.id.menu_mute_conversation:
                    handled = true;

                    for (Conversation conversation : selectedConversations) {
                        conversation.mute = true;
                        source.updateConversationSettings(conversation);
                    }

                    fragment.loadConversations();
                    break;
                case R.id.menu_pin_conversation:
                    handled = true;

                    for (Conversation conversation : selectedConversations) {
                        conversation.pinned = true;
                        source.updateConversationSettings(conversation);
                    }

                    fragment.loadConversations();
                    break;
            }

            source.close();

            mode.finish();
            return handled;
        }
    };

    public ConversationsMultiSelectDelegate(ConversationListFragment fragment) {
        super();

        this.fragment = fragment;

        if (fragment.getActivity() instanceof AppCompatActivity)
            this.activity = (AppCompatActivity) fragment.getActivity();
    }

    public void setAdapter(ConversationListAdapter adapter) {
        this.adapter = adapter;
    }

    public void startActionMode() {
        mode = activity.startSupportActionMode(actionMode);
    }

    public void clearActionMode() {
        if (mode != null) {
            mode.finish();
        }
    }
}
