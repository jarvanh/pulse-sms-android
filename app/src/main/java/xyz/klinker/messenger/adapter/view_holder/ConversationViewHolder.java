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

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.AnimationUtil;
import xyz.klinker.messenger.util.ConversationExpandedListener;

/**
 * View holder for recycling inflated conversations.
 */
public class ConversationViewHolder extends RecyclerView.ViewHolder {

    public TextView header;
    public CircleImageView image;
    public TextView name;
    public TextView summary;
    public Conversation conversation;

    private boolean expanded = false;
    private ConversationExpandedListener listener;

    public ConversationViewHolder(View itemView, ConversationExpandedListener listener) {
        super(itemView);

        this.listener = listener;

        header = (TextView) itemView.findViewById(R.id.header);
        image = (CircleImageView) itemView.findViewById(R.id.image);
        name = (TextView) itemView.findViewById(R.id.name);
        summary = (TextView) itemView.findViewById(R.id.summary);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (header == null && isBold()) {
                    setBold(false);
                }

                changeExpandedState();
            }
        });
    }

    public boolean isBold() {
        return name.getTypeface() != null && name.getTypeface().isBold();
    }

    public void setBold(boolean bold) {
        if (bold) {
            name.setTypeface(Typeface.DEFAULT_BOLD);
            summary.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            name.setTypeface(Typeface.DEFAULT);
            summary.setTypeface(Typeface.DEFAULT);
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
        expanded = true;
        AnimationUtil.expandConversationListItem(itemView);
        listener.onConversationExpanded(this);
    }

    private void collapseConversation() {
        expanded = false;
        AnimationUtil.contractConversationListItem(itemView);
        listener.onConversationContracted(this);
    }

}
