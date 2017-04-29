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

package xyz.klinker.messenger.adapter.view_holder;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.ConversationListAdapter;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.pojo.BaseTheme;
import xyz.klinker.messenger.shared.util.AnimationUtils;
import xyz.klinker.messenger.shared.util.DensityUtil;
import xyz.klinker.messenger.shared.util.listener.ContactClickedListener;
import xyz.klinker.messenger.utils.listener.ConversationExpandedListener;
import xyz.klinker.messenger.utils.multi_select.ConversationsMultiSelectDelegate;

/**
 * View holder for recycling inflated conversations.
 */
public class ConversationViewHolder extends SwappingHolder {

    public View conversationImageHolder;
    public View headerBackground;
    public TextView header;
    public ImageButton headerDone;
    public CircleImageView image;
    public TextView name;
    public TextView summary;
    public TextView imageLetter;
    public ImageView groupIcon;
    public View unreadIndicator;
    public CheckBox checkBox;

    public Conversation conversation;
    public int position = -1;

    private boolean expanded = false;
    private ConversationExpandedListener expandedListener;
    private ContactClickedListener contactClickedListener;

    public ConversationViewHolder(final View itemView, final ConversationExpandedListener listener, final ConversationListAdapter adapter) {
        super(itemView, adapter == null || adapter.getMultiSelector() == null ? new MultiSelector() : adapter.getMultiSelector());

        this.position = -1;
        this.expandedListener = listener;

        conversationImageHolder = itemView.findViewById(R.id.image_holder);
        headerBackground = itemView.findViewById(R.id.header_background);
        header = (TextView) itemView.findViewById(R.id.header);
        headerDone = (ImageButton) itemView.findViewById(R.id.section_done);
        image = (CircleImageView) itemView.findViewById(R.id.image);
        name = (TextView) itemView.findViewById(R.id.name);
        summary = (TextView) itemView.findViewById(R.id.summary);
        imageLetter = (TextView) itemView.findViewById(R.id.image_letter);
        groupIcon = (ImageView) itemView.findViewById(R.id.group_icon);
        unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);

        if (header == null) {
            setSelectionModeBackgroundDrawable(itemView.getResources().getDrawable(R.drawable.conversation_list_item_selectable_background));
        }

        itemView.setOnClickListener(view -> {
            if (header == null && ((adapter != null && adapter.getMultiSelector() != null &&
                    !adapter.getMultiSelector().tapSelection(ConversationViewHolder.this)) || adapter == null)) {
                if (conversation == null) {
                    return;
                }

                if (header == null) {
                    try {
                        adapter.getConversations().get(position).read = true;
                    } catch (Exception e) {
                    }

                    setTypeface(false, isItalic());
                }

                if (listener != null) {
                    changeExpandedState();
                }

                if (contactClickedListener != null) {
                    contactClickedListener.onClicked(
                            conversation.title, conversation.phoneNumbers, conversation.imageUri);
                }

                if (checkBox != null) {
                    checkBox.setChecked(!checkBox.isChecked());
                }
            }
        });

        itemView.setOnLongClickListener(view -> {
            if (header != null) {
                return true;
            }

            ConversationsMultiSelectDelegate multiSelect = adapter != null ? adapter.getMultiSelector() : null;
            if (multiSelect != null && !multiSelect.isSelectable()) {
                multiSelect.startActionMode();
                multiSelect.setSelectable(true);
                multiSelect.setSelected(ConversationViewHolder.this, true);
                return true;
            }

            return false;
        });

        Settings settings = Settings.get(itemView.getContext());

        if (header != null) {
            header.setTextSize(settings.smallFont);
        }

        if (name!= null && summary != null) {
            name.setTextSize(settings.largeFont);
            summary.setTextSize(settings.mediumFont);
        }

        if (settings.smallFont == 10 && conversationImageHolder != null) {
            // user selected small font from the settings
            int fourtyDp = DensityUtil.toDp(itemView.getContext(), 40);
            conversationImageHolder.getLayoutParams().height = fourtyDp;
            conversationImageHolder.getLayoutParams().width = fourtyDp;
            conversationImageHolder.invalidate();

            itemView.getLayoutParams().height = DensityUtil.toDp(itemView.getContext(), 66);
            itemView.invalidate();
        }

        if (settings.baseTheme == BaseTheme.BLACK && headerBackground != null) {
            headerBackground.setBackgroundColor(Color.BLACK);
        } else if (settings.baseTheme == BaseTheme.BLACK) {
            itemView.setBackgroundColor(Color.BLACK);
        }
    }

    public boolean isBold() {
        return name.getTypeface() != null && name.getTypeface().isBold();
    }

    public boolean isItalic() {
        return name.getTypeface() != null && name.getTypeface().getStyle() == Typeface.ITALIC;
    }

    public void setTypeface(boolean bold, boolean italic) {
        if (bold) {
            name.setTypeface(Typeface.DEFAULT_BOLD, italic ? Typeface.ITALIC : Typeface.NORMAL);
            summary.setTypeface(Typeface.DEFAULT_BOLD, italic ? Typeface.ITALIC : Typeface.NORMAL);

            if (unreadIndicator != null) {
                unreadIndicator.setVisibility(View.VISIBLE);
            }

            ((CircleImageView) unreadIndicator).setImageDrawable(new ColorDrawable(Settings.get(itemView.getContext()).globalColorSet.color));
        } else {
            name.setTypeface(Typeface.DEFAULT, italic ? Typeface.ITALIC : Typeface.NORMAL);
            summary.setTypeface(Typeface.DEFAULT, italic ? Typeface.ITALIC : Typeface.NORMAL);

            if (unreadIndicator != null) {
                unreadIndicator.setVisibility(View.GONE);
            }
        }
    }

    private void changeExpandedState() {
        if (header != null) {
            return;
        }

        if (expanded) {
            collapseConversation();
        } else {
            expandConversation();
        }
    }

    private void expandConversation() {
        if (expandedListener.onConversationExpanded(this)) {
            expanded = true;
            AnimationUtils.expandConversationListItem(itemView);
        }
    }

    private void collapseConversation() {
        expanded = false;
        expandedListener.onConversationContracted(this);
        AnimationUtils.contractConversationListItem(itemView);
    }

    public void setContactClickedListener(ContactClickedListener listener) {
        this.contactClickedListener = listener;
    }
}
