package xyz.klinker.messenger.util.multi_select;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.SelectableHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.adapter.MessageListAdapter;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.data.ArticlePreview;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.YouTubePreview;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.fragment.MessageListFragment;
import xyz.klinker.messenger.fragment.bottom_sheet.MessageShareFragment;

import static android.content.Context.CLIPBOARD_SERVICE;

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
            MenuItem info = menu.findItem(R.id.menu_message_details);
            MenuItem copy = menu.findItem(R.id.menu_copy_message);

            int checked = 0;
            for(int i = 0; i < mSelections.size(); i++) {
                int key = mSelections.keyAt(i);
                if (mSelections.get(key))
                    checked++;
                if (checked > 1)
                    break;
            }

            if (checked > 1) {
                share.setVisible(false);
                info.setVisible(false);
                copy.setVisible(false);
            } else {
                share.setVisible(true);
                info.setVisible(true);
                copy.setVisible(true);
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            clearSelections();

            // https://github.com/bignerdranch/recyclerview-multiselect/issues/9#issuecomment-140180348
            try {
                Field field = MessageMultiSelectDelegate.this.getClass().getField("mIsSelectable");
                if (field != null) {
                    if (!field.isAccessible())
                        field.setAccessible(true);
                    field.set(this, false);
                }
            } catch (Exception e) {

            }

            new Handler().postDelayed(() -> setSelectable(false), 250);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean handled = false;

            List<Long> selectedIds = new ArrayList<>();
            int highestKey = -1;
            for(int i = 0; i < mSelections.size(); i++) {
                int key = mSelections.keyAt(i);
                if (highestKey == -1 || key > highestKey)
                    highestKey = key;

                if (mSelections.get(key))
                    selectedIds.add(adapter.getItemId(key));
            }

            if (selectedIds.size() == 0) {
                return false;
            }

            if (item.getItemId() == R.id.menu_delete_messages) {
                handled = true;

                for (Long id : selectedIds) {
                    fragment.getDataSource().deleteMessage(id);
                }

                adapter.onMessageDeleted(activity, fragment.getConversationId(), highestKey);
                fragment.loadMessages();
            } else if (item.getItemId() == R.id.menu_share_message) {
                handled = true;
                Message message = fragment.getDataSource().getMessage(selectedIds.get(0));

                MessageShareFragment fragment = new MessageShareFragment();
                fragment.setMessage(message);
                fragment.show(activity.getSupportFragmentManager(), "");
            } else if (item.getItemId() == R.id.menu_copy_message) {
                handled = true;
                Message message = fragment.getDataSource().getMessage(selectedIds.get(0));

                ClipboardManager clipboard = (ClipboardManager)
                        fragment.getActivity().getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("messenger",
                        getMessageContent(message));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(fragment.getActivity(), R.string.message_copied_to_clipboard,
                        Toast.LENGTH_SHORT).show();
            } else {
                handled = true;
                new AlertDialog.Builder(activity)
                        .setMessage(fragment.getDataSource().getMessageDetails(activity, selectedIds.get(0)))
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }

            mode.finish();
            return handled;
        }
    };

    public MessageMultiSelectDelegate(MessageListFragment fragment) {
        super();

        this.fragment = fragment;

        if (fragment.getActivity() instanceof AppCompatActivity)
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
        ColorStateList states;
        int textColor;
        
        if (isActivated) {
            states = ColorStateList.valueOf(activity.getResources().getColor(R.color.actionModeBackground));
            textColor = Color.WHITE;
        } else if (message.color != -1) {
            states = ColorStateList.valueOf(message.color);
            textColor = message.textColor;
        } else {
            states = ColorStateList.valueOf(activity.getResources().getColor(R.color.drawerBackground));
            textColor = activity.getResources().getColor(R.color.primaryText);
        }
        
        if (message.messageHolder != null) {
            message.messageHolder.setBackgroundTintList(states);
        }
        
        if (message.message != null) {
            message.message.setTextColor(textColor);
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

    public static String getMessageContent(Message message) {
        if (MimeType.isExpandedMedia(message.mimeType)) {
            if (message.mimeType.equals(MimeType.MEDIA_YOUTUBE_V2)) {
                YouTubePreview preview = YouTubePreview.build(message.data);
                return preview != null ? preview.url + "\n" + preview.title : "";
            } else if (message.mimeType.equals(MimeType.MEDIA_ARTICLE)) {
                ArticlePreview preview = ArticlePreview.build(message.data);
                return preview != null ? preview.webUrl + "\n" + preview.title : "";
            }
        }

        return message.data;
    }
}
