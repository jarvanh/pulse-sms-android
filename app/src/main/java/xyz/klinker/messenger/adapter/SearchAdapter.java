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

import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.bumptech.glide.Glide;
import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;

import java.util.List;
import java.util.regex.Pattern;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.ConversationViewHolder;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.DensityUtil;
import xyz.klinker.messenger.shared.util.TimeUtils;
import xyz.klinker.messenger.shared.util.listener.SearchListener;

public class SearchAdapter extends SectionedRecyclerViewAdapter {

    private static final int VIEW_TYPE_CONVERSATION = -3;

    private String search;
    private List<Conversation> conversations;
    private List<Message> messages;
    private SearchListener listener;

    private int topMargin = -1;

    public SearchAdapter(String search, List<Conversation> conversations, List<Message> messages,
                         SearchListener listener) {
        this.search = search == null ? "" : search;
        this.conversations = conversations;
        this.messages = messages;
        this.listener = listener;
    }

    @Override
    public int getSectionCount() {
        return 2;
    }

    @Override
    public int getItemCount(int section) {
        if (section == 0) {
            if (conversations == null) {
                return 0;
            } else {
                return conversations.size();
            }
        } else {
            if (messages == null) {
                return 0;
            } else {
                return messages.size();
            }
        }
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder, int section) {
        ConversationViewHolder h = (ConversationViewHolder) holder;

        if (section == 0) {
            h.header.setText(R.string.conversations);
        } else {
            h.header.setText(R.string.messages);
        }

        if (h.headerDone != null) {
            h.headerDone.setVisibility(View.GONE);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int section,
                                 int relativePosition, int absolutePosition) {

        if (topMargin == -1) {
            topMargin = DensityUtil.toDp(holder.itemView.getContext(), 3);
        }

        Pattern pattern;
        try {
            pattern = Pattern.compile(search, Pattern.CASE_INSENSITIVE);
        } catch (Exception e) {
            //searching for a string with an invalid regex.. Ex: *^
            pattern = Pattern.compile("");
        }

        Link highlight = new Link(pattern)
                .setTextColor(holder.itemView.getContext().getResources()
                        .getColor(R.color.colorAccent))
                .setHighlightAlpha(0.4f)
                .setUnderlined(false)
                .setBold(true);

        if (holder instanceof ConversationViewHolder) {
            final Conversation conversation = conversations.get(relativePosition);

            ConversationViewHolder h = (ConversationViewHolder) holder;
            h.name.setText(conversation.title);
            h.summary.setText(conversation.snippet);

            LinkBuilder.on(h.name)
                    .addLink(highlight)
                    .build();

            if (conversation.imageUri == null) {
                h.image.setImageDrawable(new ColorDrawable(conversation.colors.color));
                if (ContactUtils.shouldDisplayContactLetter(conversation)) {
                    h.imageLetter.setText(conversation.title.substring(0, 1));
                    h.groupIcon.setVisibility(View.GONE);
                } else {
                    h.groupIcon.setVisibility(View.VISIBLE);
                    h.imageLetter.setText(null);
                }
            } else {
                h.imageLetter.setText(null);
                Glide.with(h.image.getContext())
                        .load(Uri.parse(conversation.imageUri))
                        .into(h.image);
            }

            View.OnClickListener click = view -> {
                if (listener != null) {
                    listener.onSearchSelected(conversation);
                }
            };

            if (h.itemView != null) {
                h.itemView.setOnClickListener(click);
            }

            if (h.name != null) {
                h.name.setOnClickListener(click);
            }
        } else if (holder instanceof MessageViewHolder) {
            final Message message = messages.get(relativePosition);

            MessageViewHolder h = (MessageViewHolder) holder;
            h.messageId = message.id;
            h.message.setText(message.data);

            String timestamp = TimeUtils.formatTimestamp(h.timestamp.getContext(), message.timestamp);
            if (message.from != null && !message.from.isEmpty()) {
                //noinspection AndroidLintSetTextI18n
                h.timestamp.setText(timestamp + " - " + message.from +
                        " (" + message.nullableConvoTitle + ")");
            } else {
                //noinspection AndroidLintSetTextI18n
                h.timestamp.setText(timestamp + " - " + message.nullableConvoTitle);
            }

            h.timestamp.setSingleLine(true);
            if (h.timestamp.getVisibility() != View.VISIBLE) {
                h.timestamp.setVisibility(View.VISIBLE);
            }

            LinkBuilder.on(h.message)
                    .addLink(highlight)
                    .build();

            View.OnClickListener click = view -> {
                if (listener != null) {
                    listener.onSearchSelected(message);
                }
            };

            if (h.messageHolder != null) {
                h.messageHolder.setOnClickListener(click);
            }

            if (h.message != null) {
                h.message.setOnClickListener(click);
            }

            ((ViewGroup.MarginLayoutParams) h.message.getLayoutParams()).bottomMargin = 0;
            ((ViewGroup.MarginLayoutParams) h.message.getLayoutParams()).topMargin = -1 * topMargin;
            ((ViewGroup.MarginLayoutParams) h.image.getLayoutParams()).bottomMargin = 0;
            ((ViewGroup.MarginLayoutParams) h.image.getLayoutParams()).topMargin = -1 * topMargin;
        }

    }

    @Override
    @SuppressWarnings("deprecation")
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;

        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.conversation_list_header, parent, false);
            holder = new ConversationViewHolder(view, null, null);
        } else if (viewType == VIEW_TYPE_CONVERSATION) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.conversation_list_item, parent, false);
            holder = new ConversationViewHolder(view, null, null);
        } else {
            int layoutId;
            int color;

            Settings settings = Settings.get(parent.getContext());

            if (viewType == Message.TYPE_RECEIVED) {
                layoutId = settings.rounderBubbles ? R.layout.message_received_round : R.layout.message_received;
                color = parent.getContext().getResources().getColor(R.color.colorPrimary);
            } else {
                color = -1;

                if (viewType == Message.TYPE_SENDING) {
                    layoutId = settings.rounderBubbles ? R.layout.message_sending_round : R.layout.message_sending;
                } else if (viewType == Message.TYPE_ERROR) {
                    layoutId = settings.rounderBubbles ? R.layout.message_error_round : R.layout.message_error;
                } else if (viewType == Message.TYPE_DELIVERED) {
                    layoutId = settings.rounderBubbles ? R.layout.message_delivered_round : R.layout.message_delivered;
                } else if (viewType == Message.TYPE_INFO) {
                    layoutId = R.layout.message_info;
                } else {
                    layoutId = settings.rounderBubbles ? R.layout.message_sent_round : R.layout.message_sent;
                }
            }

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(layoutId, parent, false);

            holder = new MessageViewHolder(null, view, color, -1, viewType, 0, null);
        }

        return holder;
    }

    @Override
    public int getHeaderViewType(int section) {
        //noinspection ResourceType
        return VIEW_TYPE_HEADER;
    }

    @Override
    public int getItemViewType(int section, int relativePosition, int absolutePosition) {
        if (section == 0) {
            //noinspection ResourceType
            return VIEW_TYPE_CONVERSATION;
        } else {
            return messages.get(relativePosition).type;
        }
    }

    public void updateCursors(String search, List<Conversation> conversations, List<Message> messages) {
        if (this.conversations != null) {
            this.conversations.clear();
        }

        if (this.messages != null) {
            this.messages.clear();
        }

        this.search = search == null ? "" : search;
        this.conversations = conversations;
        this.messages = messages;

        notifyDataSetChanged();
    }

}
