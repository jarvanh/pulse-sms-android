package xyz.klinker.messenger.utils.multi_select;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.SelectableHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.fragment.ArchivedConversationListFragment;
import xyz.klinker.messenger.fragment.ConversationListFragment;
import xyz.klinker.messenger.shared.data.pojo.BaseTheme;

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

            new Handler().postDelayed(() -> setSelectable(false), 250);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean handled = false;

            List<Integer> selectedPositions = new ArrayList<>();
            List<Conversation> selectedConversations = new ArrayList<>();
            for (int i = 0; i < adapter.getItemCount(); i++) {
                if (isSelected(i, 0)) {
                    selectedPositions.add(i);
                    if (adapter.showHeaderAboutTextingOnline()) {
                        selectedConversations.add(adapter.findConversationForPosition(i + 1));
                    } else {
                        selectedConversations.add(adapter.findConversationForPosition(i));
                    }
                }
            }

            DataSource source = DataSource.getInstance(activity);

            switch (item.getItemId()) {
                case R.id.menu_archive_conversation:
                    handled = true;

                     int removed = 0;
                    for (int i = 0; i < adapter.getItemCount(); i++) {
                        if (isSelected(i + removed, 0)) {
                            boolean removedHeader = adapter.archiveItem(i);
                            removed += removedHeader ? 2 : 1;
                            i--;
                        }
                    }

                    break;
                case R.id.menu_delete_conversation:
                    handled = true;

                    removed = 0;
                    for (int i = 0; i < adapter.getItemCount(); i++) {
                        if (isSelected(i + removed, 0)) {
                            boolean removedHeader = adapter.deleteItem(i);
                            removed += removedHeader ? 2 : 1;
                            i--;
                        }
                    }

                    break;
                case R.id.menu_mute_conversation:
                    handled = true;

                    for (Conversation conversation : selectedConversations) {
                        conversation.mute = !conversation.mute;
                        source.updateConversationSettings(conversation);
                    }

                    fragment.loadConversations();
                    break;
                case R.id.menu_pin_conversation:
                    handled = true;

                    for (Conversation conversation : selectedConversations) {
                        conversation.pinned = !conversation.pinned;
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

    @Override
    protected void refreshHolder(SelectableHolder holder) {
        if (holder == null || !(holder instanceof ConversationViewHolder) || !isSelectable() ||
                Settings.get(fragment.getActivity()).baseTheme != BaseTheme.BLACK) {
            super.refreshHolder(holder);
            return;
        }

        ConversationViewHolder conversation = (ConversationViewHolder) holder;
        conversation.setSelectable(mIsSelectable);

        boolean isActivated = mSelections.get(conversation.getAdapterPosition());
        ColorStateList states;

        if (isActivated) {
            states = ColorStateList.valueOf(activity.getResources().getColor(R.color.actionModeBackground));
        } else {
            states = ColorStateList.valueOf(Color.BLACK);
        }

        if (conversation.itemView != null) {
            conversation.itemView.setBackgroundTintList(states);
        }
    }

    @Override
    public boolean tapSelection(SelectableHolder holder) {
        boolean result = super.tapSelection(holder);

        if (mode != null && Settings.get(fragment.getActivity()).baseTheme != BaseTheme.BLACK) {
            mode.invalidate();
        }

        return result;
    }
}
