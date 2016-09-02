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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;
import com.klinker.android.link_builder.TouchableMovementMethod;

import java.util.regex.Pattern;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.fragment.MessageListFragment;
import xyz.klinker.messenger.util.ImageUtils;
import xyz.klinker.messenger.util.PhoneNumberUtils;
import xyz.klinker.messenger.util.Regex;
import xyz.klinker.messenger.util.TimeUtils;

/**
 * Adapter for displaying messages in a conversation.
 */
public class MessageListAdapter extends RecyclerView.Adapter<MessageViewHolder> {

    private Cursor messages;
    private int receivedColor;
    private int accentColor;
    private boolean isGroup;
    private LinearLayoutManager manager;
    private MessageListFragment fragment;
    private int timestampHeight;

    public MessageListAdapter(Cursor messages, int receivedColor, int accentColor, boolean isGroup,
                              LinearLayoutManager manager, MessageListFragment fragment) {
        this.messages = messages;
        this.receivedColor = receivedColor;
        this.accentColor = accentColor;
        this.isGroup = isGroup;
        this.manager = manager;
        this.fragment = fragment;
        this.timestampHeight = 0;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId;
        int color;

        if (timestampHeight == 0) {
            setTimestampHeight(parent.getResources()
                    .getDimensionPixelSize(R.dimen.timestamp_height));
        }

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
            } else if (viewType == Message.TYPE_INFO) {
                layoutId = R.layout.message_info;
            } else {
                layoutId = R.layout.message_sent;
            }
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        messages.moveToFirst();
        return new MessageViewHolder(fragment, view, color,
                messages.getLong(messages.getColumnIndex(Message.COLUMN_CONVERSATION_ID)),
                viewType, timestampHeight);
    }

    @VisibleForTesting
    void setTimestampHeight(int height) {
        this.timestampHeight = height;
    }

    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        messages.moveToPosition(position);
        Message message = new Message();
        message.fillFromCursor(messages);

        holder.messageId = message.id;

        if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
            holder.message.setText(message.data);

            Link urls = new Link(Regex.WEB_URL);
            urls.setTextColor(accentColor);
            urls.setHighlightAlpha(.4f);
            urls.setOnClickListener(new Link.OnClickListener() {
                @Override
                public void onClick(String clickedText) {
                    if (!clickedText.startsWith("http")) {
                        clickedText = "http://" + clickedText;
                    }

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickedText));
                    holder.message.getContext().startActivity(browserIntent);
                }
            });

            Link phoneNumbers = new Link(Regex.PHONE);
            phoneNumbers.setTextColor(accentColor);
            phoneNumbers.setHighlightAlpha(.4f);
            phoneNumbers.setOnClickListener(new Link.OnClickListener() {
                @Override
                public void onClick(String clickedText) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + PhoneNumberUtils.clearFormatting(clickedText)));
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
            holder.image.setImageDrawable(null);
            holder.image.setMinimumWidth(0);
            if (MimeType.isStaticImage(message.mimeType)) {
                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.data))
                        .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                        .fitCenter()
                        .into(holder.image);
            } else if (message.mimeType.equals(MimeType.IMAGE_GIF)) {
                holder.image.setMaxWidth(holder.image.getContext()
                        .getResources().getDimensionPixelSize(R.dimen.max_gif_width));
                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.data))
                        .fitCenter()
                        .into(holder.image);
            } else if (MimeType.isVideo(message.mimeType)) {
                Drawable placeholder;
                if (getItemViewType(position) != Message.TYPE_RECEIVED) {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_play_sent);
                } else {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_play);
                }

                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.data))
                        .asBitmap()
                        .error(placeholder)
                        .placeholder(placeholder)
                        .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                        .fitCenter()
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource,
                                                        GlideAnimation<? super Bitmap> glideAnimation) {
                                ImageUtils.overlayBitmap(holder.image.getContext(),
                                        resource, R.drawable.ic_play);
                                holder.image.setImageBitmap(resource);
                            }
                        });
            } else if (MimeType.isAudio(message.mimeType)) {
                Drawable placeholder;
                if (getItemViewType(position) != Message.TYPE_RECEIVED) {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_audio_sent);
                } else {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_audio);
                }

                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.data))
                        .error(placeholder)
                        .placeholder(placeholder)
                        .into(holder.image);
            } else {
                Log.v("MessageListAdapter", "unused mime type: " + message.mimeType);
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

        holder.timestamp.setText(TimeUtils.formatTimestamp(holder.timestamp.getContext(),
                message.timestamp));

        if (TimeUtils.shouldDisplayTimestamp(message.timestamp, nextTimestamp)) {
            holder.timestamp.getLayoutParams().height = timestampHeight;
        } else {
            holder.timestamp.getLayoutParams().height = 0;
        }

        if (isGroup && holder.contact != null && message.from != null) {
            if (holder.contact.getVisibility() == View.GONE) {
                holder.contact.setVisibility(View.VISIBLE);
            }

            int label = holder.timestamp.getLayoutParams().height > 0 ?
                    R.string.message_from_bullet : R.string.message_from;
            holder.contact.setText(holder.contact.getResources().getString(label, message.from));
        }
    }

    @Override
    public int getItemCount() {
        if (messages == null || messages.isClosed() || !messages.moveToFirst()) {
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

    public long getItemId(int position) {
        messages.moveToPosition(position);
        return messages.getLong(messages.getColumnIndex(Message.COLUMN_ID));
    }

    public void addMessage(Cursor newMessages) {
        int initialCount = getItemCount();

        messages = newMessages;

        if (newMessages != null) {
            int finalCount = getItemCount();

            if (initialCount == finalCount) {
                notifyItemChanged(finalCount - 1);
            } else if (initialCount > finalCount) {
                notifyDataSetChanged();
            } else {
                notifyItemInserted(finalCount - 1);
                manager.scrollToPosition(finalCount - 1);
            }
        }
    }

    public Cursor getMessages() {
        return messages;
    }

}
