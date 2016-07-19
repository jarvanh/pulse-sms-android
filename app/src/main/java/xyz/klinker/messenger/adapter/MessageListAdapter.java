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

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;
import com.klinker.android.link_builder.TouchableMovementMethod;

import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.PhoneNumberUtil;
import xyz.klinker.messenger.util.TimeUtil;

/**
 * Adapter for displaying messages in a conversation.
 */
public class MessageListAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private Cursor messages;
    private int receivedColor;
    private int accentColor;
    private boolean isGroup;
    private LinearLayoutManager manager;

    public MessageListAdapter(Cursor messages, int receivedColor, int accentColor, boolean isGroup,
                              LinearLayoutManager manager) {
        this.messages = messages;
        this.receivedColor = receivedColor;
        this.accentColor = accentColor;
        this.isGroup = isGroup;
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
            color = -1;

            if (viewType == Message.TYPE_SENDING) {
                layoutId = R.layout.message_sending;
            } else if (viewType == Message.TYPE_ERROR) {
                layoutId = R.layout.message_error;
            } else if (viewType == Message.TYPE_DELIVERED) {
                layoutId = R.layout.message_delivered;
            } else {
                layoutId = R.layout.message_sent;
            }
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        return new MessageViewHolder(view, color);
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        messages.moveToPosition(position);
        Message message = new Message();
        message.fillFromCursor(messages);

        if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
            holder.message.setText(message.data);

            Link urls = new Link(Patterns.WEB_URL);
            urls.setTextColor(accentColor);
            urls.setHighlightAlpha(.4f);
            urls.setOnClickListener(new Link.OnClickListener() {
                @Override
                public void onClick(String clickedText) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedText));
                    holder.message.getContext().startActivity(browserIntent);
                }
            });

            Link phoneNumbers = new Link(Patterns.PHONE);
            phoneNumbers.setTextColor(accentColor);
            phoneNumbers.setHighlightAlpha(.4f);
            phoneNumbers.setOnClickListener(new Link.OnClickListener() {
                @Override
                public void onClick(String clickedText) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + PhoneNumberUtil.clearFormatting(clickedText)));
                    holder.message.getContext().startActivity(intent);
                }
            });

            if (holder.message.getMovementMethod() == null) {
                holder.message.setMovementMethod(new TouchableMovementMethod());
            }

            LinkBuilder.on(holder.message).addLink(urls).addLink(phoneNumbers).build();

            if (holder.image.getVisibility() == View.VISIBLE) {
                holder.image.setVisibility(View.GONE);
            }

            if (holder.message.getVisibility() == View.GONE) {
                holder.message.setVisibility(View.VISIBLE);
            }
        } else {
            if (message.mimeType.startsWith("image/")) {
                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.data))
                        .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                        .fitCenter()
                        .into(holder.image);
            } else {
                Log.v("MessageListAdapter", "unused mime type: " + message.mimeType);
                // TODO video and audio, etc.
            }

            if (holder.message.getVisibility() == View.VISIBLE) {
                holder.message.setVisibility(View.GONE);
            }

            if (holder.image.getVisibility() == View.GONE) {
                holder.image.setVisibility(View.VISIBLE);
            }
        }

        long nextTimestamp;
        if (position != getItemCount() - 1) {
            messages.moveToPosition(position + 1);
            nextTimestamp = messages.getLong(messages.getColumnIndex(Message.COLUMN_TIMESTAMP));
        } else {
            nextTimestamp = System.currentTimeMillis();
        }

        if (TimeUtil.shouldDisplayTimestamp(message.timestamp, nextTimestamp)) {
            holder.timestamp.setVisibility(View.VISIBLE);
            holder.timestamp.setText(TimeUtil.formatTimestamp(holder.timestamp.getContext(),
                    message.timestamp));
        } else {
            holder.timestamp.setVisibility(View.GONE);
        }

        if (isGroup && holder.contact != null && message.from != null) {
            if (holder.contact.getVisibility() == View.GONE) {
                holder.contact.setVisibility(View.VISIBLE);
            }

            int label = holder.timestamp.getVisibility() == View.VISIBLE ?
                    R.string.message_from_bullet : R.string.message_from;
            holder.contact.setText(holder.contact.getResources().getString(label, message.from));
        }
    }

    @Override
    public int getItemCount() {
        if (messages == null || !messages.moveToFirst()) {
            return 0;
        } else {
            return messages.getCount();
        }
    }

    @Override
    public int getItemViewType(int position) {
        messages.moveToPosition(position);
        return messages.getInt(messages.getColumnIndex(Message.COLUMN_TYPE));
    }

    public void addMessage(Cursor newMessages) {
        messages = newMessages;

        if (newMessages != null) {
            notifyItemInserted(messages.getCount() - 1);
            manager.scrollToPosition(messages.getCount() - 1);
        }
    }

    public Cursor getMessages() {
        return messages;
    }

}
