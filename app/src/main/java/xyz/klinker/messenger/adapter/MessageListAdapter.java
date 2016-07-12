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

package xyz.klinker.messenger.adapter;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.data.Message;
import xyz.klinker.messenger.util.TimeUtil;

/**
 * Adapter for displaying messages in a conversation.
 */
public class MessageListAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private List<Message> messages;
    private int receivedColor;
    private LinearLayoutManager manager;

    public MessageListAdapter(List<Message> messages, int receivedColor,
                              LinearLayoutManager manager) {
        this.messages = messages;
        this.receivedColor = receivedColor;
        this.manager = manager;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId;
        int color;

        if (viewType == Message.TYPE_RECEIVED) {
            layoutId = R.layout.message_received;
            color = receivedColor;
        } else {
            layoutId = R.layout.message_sent;
            color = -1;
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        return new MessageViewHolder(view, color);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        if (message.mimetype.equals("text/plain")) {
            holder.message.setText(message.data);
        } else {
            // handle other mime types here
        }

        long nextTimestamp;
        if (position != getItemCount() - 1) {
            nextTimestamp = messages.get(position + 1).timestamp;
        } else {
            nextTimestamp = System.currentTimeMillis();
        }

        if (TimeUtil.shouldDisplayTimestamp(message.timestamp, nextTimestamp)) {
            holder.timestamp.setVisibility(View.VISIBLE);
            holder.timestamp.setText(TimeUtil.formatTimestamp(message.timestamp));
        } else {
            holder.timestamp.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        if (messages == null) {
            return 0;
        } else {
            return messages.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).type;
    }

    public void addMessage(Message message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
        manager.scrollToPosition(messages.size() - 1);
    }

}
