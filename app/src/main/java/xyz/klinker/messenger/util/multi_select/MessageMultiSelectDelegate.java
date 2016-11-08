package xyz.klinker.messenger.util.multi_select;

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
import xyz.klinker.messenger.adapter.MessageListAdapter;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.fragment.MessageListFragment;

public class MessageMultiSelectDelegate extends MultiSelector {

    private AppCompatActivity activity;
    private MessageListFragment fragment;
    private MessageListAdapter adapter;

    private ActionMode mode;
    private ActionMode.Callback actionMode = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            clearSelections();
            setSelectable(true);

            activity.getMenuInflater().inflate(R.menu.action_mode_message_list, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            MenuItem delete = menu.findItem(R.id.menu_delete_messages);
            MenuItem share = menu.findItem(R.id.menu_share_message);
            MenuItem info = menu.findItem(R.id.menu_message_info);

            int checked = 0;
            for(int i = 0; i < mSelections.size(); i++) {
                int key = mSelections.keyAt(i);
                if (mSelections.get(key))
                    checked++;
                if (checked > 1)
                    break;
            }

            if (checked > 1) {
                share.setVisible(true);
                info.setVisible(true);
            } else {
                share.setVisible(false);
                info.setVisible(false);
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            clearSelections();

            // https://github.com/bignerdranch/recyclerview-multiselect/issues/9#issuecomment-140180348
            try {
                Field field = MessageMultiSelectDelegate.this.getClass().getDeclaredField("mIsSelectable");
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

            mode.finish();
            return handled;
        }
    };

    public MessageMultiSelectDelegate(MessageListFragment fragment) {
        super();

        this.fragment = fragment;
        this.activity = (AppCompatActivity) fragment.getActivity();
    }

    public void setAdapter(MessageListAdapter adapter) {
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
        if (holder == null || !(holder instanceof MessageViewHolder) || !isSelectable()) {
            return;
        }

        MessageViewHolder message = (MessageViewHolder) holder;
        message.setSelectable(mIsSelectable);

        boolean isActivated = mSelections.get(message.getAdapterPosition());
        if (isActivated) {
            message.messageHolder.setBackgroundTintList(ColorStateList.valueOf(activity.getResources().getColor(R.color.actionModeBackground)));
            message.message.setTextColor(Color.WHITE);
        } else if (message.color != -1) {
            message.messageHolder.setBackgroundTintList(ColorStateList.valueOf(message.color));
            message.message.setTextColor(message.textColor);
        } else {
            message.messageHolder.setBackgroundTintList(ColorStateList.valueOf(activity.getResources().getColor(R.color.drawerBackground)));
            message.message.setTextColor(activity.getResources().getColor(R.color.primaryText));
        }
    }

    @Override
    public boolean tapSelection(SelectableHolder holder) {
        boolean result = super.tapSelection(holder);

        if (mode != null) {
            mode.invalidate();
        }

        return result;
    }
}
