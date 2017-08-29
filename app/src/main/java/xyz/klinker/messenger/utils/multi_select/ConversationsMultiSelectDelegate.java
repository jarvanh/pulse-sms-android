package xyz.klinker.messenger.utils.multi_select;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.TooltipCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

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
import xyz.klinker.messenger.shared.util.DensityUtil;

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

                MenuItem unarchive = menu.findItem(R.id.menu_archive_conversation);
                fixMenuItemLongClickCrash(actionMode, unarchive, R.drawable.ic_unarchive, R.string.menu_move_to_inbox);
            } else {
                activity.getMenuInflater().inflate(R.menu.action_mode_conversation_list, menu);

                MenuItem archive = menu.findItem(R.id.menu_archive_conversation);
                MenuItem pin = menu.findItem(R.id.menu_pin_conversation);
                MenuItem mute = menu.findItem(R.id.menu_mute_conversation);

                fixMenuItemLongClickCrash(actionMode, archive, R.drawable.ic_archive, R.string.menu_archive_conversation);
                fixMenuItemLongClickCrash(actionMode, pin, R.drawable.ic_pin, R.string.pin_conversation);
                fixMenuItemLongClickCrash(actionMode, mute, R.drawable.ic_mute, R.string.mute_conversation);
            }

            MenuItem delete = menu.findItem(R.id.menu_delete_conversation);
            fixMenuItemLongClickCrash(actionMode, delete, R.drawable.ic_delete, R.string.menu_delete_conversation);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            int checked = 0;
            for(int i = 0; i < mSelections.size(); i++) {
                int key = mSelections.keyAt(i);
                if (mSelections.get(key))
                    checked++;
                if (checked > 1)
                    break;
            }

            if (checked == 0) {
                clearActionMode();
            }

            return false;
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

                    try {
                        if (adapter.showHeaderAboutTextingOnline()) {
                            selectedConversations.add(adapter.findConversationForPosition(i - 1));
                        } else {
                            selectedConversations.add(adapter.findConversationForPosition(i));
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {

                    }
                }
            }

            DataSource source = DataSource.INSTANCE;

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
                        source.updateConversationSettings(activity, conversation);
                    }

                    fragment.loadConversations();
                    break;
                case R.id.menu_pin_conversation:
                    handled = true;

                    for (Conversation conversation : selectedConversations) {
                        conversation.pinned = !conversation.pinned;
                        source.updateConversationSettings(activity, conversation);
                    }

                    fragment.loadConversations();
                    break;
            }

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


    private void fixMenuItemLongClickCrash(ActionMode mode, MenuItem item, int icon, int text) {
        try {
            ImageView image = new ImageView(activity);
            image.setImageResource(icon);
            image.setPaddingRelative(0, 0, DensityUtil.toDp(activity, 24), 0);
            item.setActionView(image);
            TooltipCompat.setTooltipText(item.getActionView(), activity.getString(text));

            image.setOnClickListener(view -> {
                actionMode.onActionItemClicked(mode, item);
            });

            image.setOnLongClickListener(view -> {
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
