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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;
import com.klinker.android.link_builder.TouchableMovementMethod;
import com.turingtechnologies.materialscrollbar.IDateableAdapter;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import xyz.klinker.android.article.ArticleIntent;
import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.ArticlePreview;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.YouTubePreview;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.fragment.MessageListFragment;
import xyz.klinker.messenger.fragment.bottom_sheet.LinkLongClickFragment;
import xyz.klinker.messenger.shared.util.ColorUtils;
import xyz.klinker.messenger.shared.util.DensityUtil;
import xyz.klinker.messenger.shared.util.ImageUtils;
import xyz.klinker.messenger.shared.util.MessageListStylingHelper;
import xyz.klinker.messenger.shared.util.PhoneNumberUtils;
import xyz.klinker.messenger.shared.util.Regex;
import xyz.klinker.messenger.shared.util.SnackbarAnimationFix;
import xyz.klinker.messenger.shared.util.TimeUtils;
import xyz.klinker.messenger.shared.util.listener.MessageDeletedListener;
import xyz.klinker.messenger.shared.util.media.parsers.ArticleParser;

/**
 * Adapter for displaying messages in a conversation.
 */
public class MessageListAdapter extends RecyclerView.Adapter<MessageViewHolder>
        implements MessageDeletedListener, IDateableAdapter {

    private static final String TAG = "MessageListAdapter";

    private float largeFont;
    private Cursor messages;
    private Map<String, Contact> fromColorMapper;
    private Map<String, Contact> fromColorMapperByName;
    private int receivedColor;
    private int accentColor;
    private boolean isGroup;
    private boolean ignoreSendingStatus;
    private LinearLayoutManager manager;
    private MessageListFragment fragment;
    private int timestampHeight;

    private MessageListStylingHelper stylingHelper;

    private int imageHeight;
    private int imageWidth;

    public Snackbar snackbar;

    public MessageListAdapter(Cursor messages, int receivedColor, int accentColor, boolean isGroup,
                              LinearLayoutManager manager, MessageListFragment fragment) {
        this.messages = messages;
        this.receivedColor = receivedColor;
        this.accentColor = accentColor;
        this.isGroup = isGroup;
        this.manager = manager;
        this.fragment = fragment;
        this.timestampHeight = 0;

        if (fragment.getActivity() == null) {
            imageHeight = imageWidth = 50;
        } else {
            imageHeight = imageWidth = DensityUtil.toPx(fragment.getActivity(), 350);
        }

        try {
            Account account = Account.get(fragment.getActivity());
            ignoreSendingStatus = account.exists() && !account.primary;
        } catch (Exception e) {
            ignoreSendingStatus = false;
        }

        stylingHelper = new MessageListStylingHelper(fragment.getActivity());

        if (fragment.getMultiSelect() != null)
            fragment.getMultiSelect().setAdapter(this);

        largeFont = Settings.get(fragment.getActivity()).largeFont;
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId;
        int color;

        if (timestampHeight == 0) {
            setTimestampHeight(DensityUtil.spToPx(parent.getContext(),
                    Settings.get(parent.getContext()).mediumFont + 2));
        }

        boolean rounder = Settings.get(parent.getContext()).rounderBubbles;
        if (viewType == Message.TYPE_RECEIVED) {
            layoutId = rounder ? R.layout.message_received_round : R.layout.message_received;
            color = receivedColor;
        } else {
            color = -1;

            if (viewType == Message.TYPE_SENDING) {
                layoutId = rounder ? R.layout.message_sending_round : R.layout.message_sending;
            } else if (viewType == Message.TYPE_ERROR) {
                layoutId = rounder ? R.layout.message_error_round : R.layout.message_error;
            } else if (viewType == Message.TYPE_DELIVERED) {
                layoutId = rounder ? R.layout.message_delivered_round : R.layout.message_delivered;
            } else if (viewType == Message.TYPE_INFO) {
                layoutId = R.layout.message_info;
            } else if (viewType == Message.TYPE_MEDIA) {
                layoutId = R.layout.message_media;
            } else {
                layoutId = rounder ? R.layout.message_sent_round : R.layout.message_sent;
            }
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        messages.moveToFirst();
        MessageViewHolder holder = new MessageViewHolder(fragment, view, fromColorMapper != null && fromColorMapper.size() > 1 ? -1 : color,
                messages.getLong(messages.getColumnIndex(Message.COLUMN_CONVERSATION_ID)),
                viewType, timestampHeight, this);

        holder.setColors(receivedColor, accentColor);

        return holder;
    }

    @VisibleForTesting
    void setTimestampHeight(int height) {
        this.timestampHeight = height;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        messages.moveToPosition(position);
        Message message = new Message();
        message.fillFromCursor(messages);

        holder.messageId = message.id;
        holder.mimeType = message.mimeType;
        holder.data = message.data;

        colorMessage(holder, message);

        if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
            holder.message.setText(message.data);

            if (!message.data.isEmpty() && message.data.replaceAll(Regex.EMOJI, "").isEmpty()) {
                // enlarge emojis
                holder.message.setTextSize(35);
            } else {
                holder.message.setTextSize(largeFont);
            }

            Link urls = new Link(Regex.WEB_URL);
            urls.setTextColor(accentColor);
            urls.setHighlightAlpha(.4f);

            urls.setOnLongClickListener(clickedText -> {
                if (!clickedText.startsWith("http")) {
                    clickedText = "http://" + clickedText;
                }

                LinkLongClickFragment bottomSheet = new LinkLongClickFragment();
                bottomSheet.setColors(receivedColor, accentColor);
                bottomSheet.setLink(clickedText);
                bottomSheet.show(fragment.getActivity().getSupportFragmentManager(), "");
            });

            urls.setOnClickListener(clickedText -> {
                if (fragment.getMultiSelect().isSelectable()) {
                    holder.messageHolder.performClick();
                    return;
                }

                if (!clickedText.startsWith("http")) {
                    clickedText = "http://" + clickedText;
                }

                if (clickedText.contains("youtube") || !Settings.get(holder.itemView.getContext()).internalBrowser) {
                    Intent url = new Intent(Intent.ACTION_VIEW);
                    url.setData(Uri.parse(clickedText));
                    holder.itemView.getContext().startActivity(url);
                } else {
                    ArticleIntent intent = new ArticleIntent.Builder(holder.itemView.getContext(), ArticleParser.ARTICLE_API_KEY)
                            .setToolbarColor(receivedColor)
                            .setAccentColor(accentColor)
                            .setTheme(Settings.get(holder.itemView.getContext()).isCurrentlyDarkTheme() ?
                                    ArticleIntent.THEME_DARK : ArticleIntent.THEME_LIGHT)
                            .setTextSize(Settings.get(holder.itemView.getContext()).mediumFont + 1)
                            .build();

                    intent.launchUrl(holder.itemView.getContext(), Uri.parse(clickedText));
                }
            });

            Link phoneNumbers = new Link(Regex.PHONE);
            phoneNumbers.setTextColor(accentColor);
            phoneNumbers.setHighlightAlpha(.4f);
            phoneNumbers.setOnClickListener(clickedText -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + PhoneNumberUtils.clearFormatting(clickedText)));
                holder.message.getContext().startActivity(intent);
            });

            Link emails = new Link(Patterns.EMAIL_ADDRESS);
            emails.setTextColor(accentColor);
            emails.setHighlightAlpha(.4f);
            emails.setOnClickListener(clickedText -> {
                String[] email = new String[]{clickedText};
                Uri uri = Uri.parse("mailto:" + clickedText);

                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, uri);
                emailIntent.putExtra(Intent.EXTRA_EMAIL, email);
                holder.message.getContext().startActivity(emailIntent);
            });

            if (holder.message.getMovementMethod() == null) {
                holder.message.setMovementMethod(new TouchableMovementMethod());
            }

            LinkBuilder.on(holder.message).addLink(emails).addLink(urls).addLink(phoneNumbers).build();

            setGone(holder.image);
            setVisible(holder.message);
        } else if (!MimeType.isExpandedMedia(message.mimeType)) {
            holder.image.setImageDrawable(null);
            holder.image.setMinimumWidth(imageWidth);
            holder.image.setMinimumHeight(imageHeight);

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
                        .load(Uri.parse(message.data)).fitCenter()
                        .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE)
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
            } else if (MimeType.isVcard(message.mimeType)) {
                holder.message.setText(message.data);
                holder.image.setImageResource(getItemViewType(position) != Message.TYPE_RECEIVED ?
                        R.drawable.ic_contacts_sent : R.drawable.ic_contacts);
            } else {
                Log.v("MessageListAdapter", "unused mime type: " + message.mimeType);
            }

            setGone(holder.message);
            setVisible(holder.image);
        } else {
            if (message.mimeType.equals(MimeType.MEDIA_YOUTUBE)) {
                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.data))
                        .asBitmap()
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
                setGone(holder.message);
                setGone(holder.clippedImage);
                setGone(holder.title);
                setGone(holder.contact);
            } else if (message.mimeType.equals(MimeType.MEDIA_YOUTUBE_V2)) {
                YouTubePreview preview = YouTubePreview.build(message.data);
                if (preview != null) {
                    Glide.with(holder.clippedImage.getContext())
                            .load(Uri.parse(preview.thumbnail))
                            .asBitmap()
                            .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                            .fitCenter()
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap resource,
                                                            GlideAnimation<? super Bitmap> glideAnimation) {
                                    ImageUtils.overlayBitmap(holder.image.getContext(),
                                            resource, R.drawable.ic_play);
                                    holder.clippedImage.setImageBitmap(resource);
                                }
                            });

                    holder.contact.setText(preview.title);
                    holder.title.setText("YouTube");

                    setGone(holder.image);
                    setGone(holder.message);
                    setVisible(holder.clippedImage);
                    setVisible(holder.contact);
                    setVisible(holder.title);
                } else {
                    setGone(holder.clippedImage);
                    setGone(holder.image);
                    setGone(holder.message);
                    setGone(holder.timestamp);
                    setGone(holder.title);
                }
            } else if (message.mimeType.equals(MimeType.MEDIA_TWITTER)) {

            } else if (message.mimeType.equals(MimeType.MEDIA_ARTICLE)) {
                ArticlePreview preview = ArticlePreview.build(message.data);
                if (preview != null) {
                    Glide.with(holder.clippedImage.getContext())
                            .load(Uri.parse(preview.imageUrl))
                            .asBitmap()
                            .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                            .fitCenter()
                            .into(holder.clippedImage);

                    holder.contact.setText(preview.title);
                    holder.message.setText(preview.description);
                    holder.title.setText(preview.domain);

                    setGone(holder.image);
                    setVisible(holder.clippedImage);
                    setVisible(holder.contact);
                    setVisible(holder.message);
                    setVisible(holder.title);
                } else {
                    setGone(holder.clippedImage);
                    setGone(holder.image);
                    setGone(holder.message);
                    setGone(holder.timestamp);
                    setGone(holder.title);
                }
            }

            setVisible(holder.image);
        }

        if (message.simPhoneNumber != null) {
            holder.timestamp.setText(TimeUtils.formatTimestamp(holder.timestamp.getContext(),
                    message.timestamp) + " (SIM " + message.simPhoneNumber + ")");
        } else {
            holder.timestamp.setText(TimeUtils.formatTimestamp(holder.timestamp.getContext(),
                    message.timestamp));
        }

        if (!isGroup) {
            stylingHelper.calculateAdjacentItems(messages, position)
                    .setMargins(holder.itemView)
                    .setBackground(holder.messageHolder, message.mimeType)
                    .applyTimestampHeight(holder.timestamp, timestampHeight);
        } else {
            stylingHelper.calculateAdjacentItems(messages, position)
                    .applyTimestampHeight(holder.timestamp, timestampHeight);
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
        try {
            return messages.getCount();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (messages == null || messages.getCount() == 0 || messages.isClosed()) {
            return -1;
        }

        messages.moveToPosition(position);
        int type = messages.getInt(messages.getColumnIndex(Message.COLUMN_TYPE));
        if (ignoreSendingStatus && type == Message.TYPE_SENDING) {
            type = Message.TYPE_SENT;
        }

        return type;
    }

    @Override
    public Date getDateForElement(int position) {
        if (position < 0) {
            return new Date();
        }

        messages.moveToPosition(position);
        long millis = messages.getLong(messages.getColumnIndex(Message.COLUMN_TIMESTAMP));
        return new Date(millis);
    }

    private void setVisible(View v) {
        if (v != null && v.getVisibility() != View.VISIBLE) {
            v.setVisibility(View.VISIBLE);
        }
    }

    private void setGone(View v) {
        if (v != null && v.getVisibility() != View.GONE) {
            v.setVisibility(View.GONE);
        }
    }

    public long getItemId(int position) {
        if (messages == null || messages.getCount() == 0) {
            return -1;
        }

        messages.moveToPosition(position);
        return messages.getLong(messages.getColumnIndex(Message.COLUMN_ID));
    }

    public void addMessage(RecyclerView recycler, Cursor newMessages) {
        int initialCount = getItemCount();

        try {
            messages.close();
        } catch (Exception e) {
        }

        messages = newMessages;

        if (newMessages != null) {
            int finalCount = getItemCount();

            if (initialCount == finalCount) {
                notifyItemChanged(finalCount - 1);
            } else if (initialCount > finalCount) {
                notifyDataSetChanged();
            } else {
                if (finalCount - 2 >= 0) {
                    // with the new paddings, we need to notify the second to last item too
                    notifyItemChanged(finalCount - 2);
                }

                notifyItemInserted(finalCount - 1);

                if (Math.abs(manager.findLastVisibleItemPosition() - initialCount) < 4) {
                    // near the bottom, scroll to the new item
                    manager.scrollToPosition(finalCount - 1);
                } else if (recycler != null && FeatureFlags.get(recycler.getContext()).RECEIVED_MESSAGE_SNACKBAR && messages.moveToLast()) {
                    Message message = new Message();
                    message.fillFromCursor(messages);
                    if (message.type == Message.TYPE_RECEIVED) {
                        String text = recycler.getContext().getString(R.string.new_message);
                        snackbar = Snackbar
                                .make(recycler, text, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.read, view -> manager.scrollToPosition(finalCount - 1));

                        ((CoordinatorLayout.LayoutParams) snackbar.getView().getLayoutParams())
                                .bottomMargin = DensityUtil.toDp(recycler.getContext(), 56);

                        SnackbarAnimationFix.apply(snackbar);
                        snackbar.show();
                    }
                }
            }
        }
    }

    public Cursor getMessages() {
        return messages;
    }

    @Override
    public void onMessageDeleted(Context context, long conversationId, int position) {
        if (position == getItemCount() - 1 && position != 0) {
            Log.v(TAG, "deleted last item, updating conversation");
            DataSource source = DataSource.getInstance(context);
            source.open();

            Message message = new Message();
            messages.moveToPosition(position - 1);
            message.fillFromCursor(messages);

            Conversation conversation = source.getConversation(conversationId);
            source.updateConversation(conversationId, conversation.read, message.timestamp,
                    message.type == Message.TYPE_SENT || message.type == Message.TYPE_SENDING ?
                            context.getString(R.string.you) + ": " + message.data : message.data,
                    message.mimeType, conversation.archive);

            fragment.setConversationUpdateInfo(message.type == Message.TYPE_SENDING ?
                    context.getString(R.string.you) + ": " + message.data : message.data);

            source.close();
        } else if (position == 0 && (getItemCount() == 1 || getItemCount() == 0)) {
            ((MessengerActivity) fragment.getActivity()).menuItemClicked(R.id.menu_delete_conversation);
        } else {
            Log.v(TAG, "position not last, so leaving conversation");
        }
    }

    public void setFromColorMapper(Map<String, Contact> colorMapper, Map<String, Contact> colorMapperByName) {
        this.fromColorMapper = colorMapper;
        this.fromColorMapperByName = colorMapperByName;
    }

    private void colorMessage(final MessageViewHolder holder, final Message message) {
        if (Settings.get(holder.itemView.getContext()).useGlobalThemeColor) {
            return;
        }

        if (message.type == Message.TYPE_RECEIVED &&
                fromColorMapper != null && fromColorMapper.size() > 1) { // size > 1 so we know it is a group convo
            if (fromColorMapper.containsKey(message.from)) {
                // group convo, color them differently
                // this is the usual result
                int color = fromColorMapper.get(message.from).colors.color;
                holder.messageHolder.setBackgroundTintList(
                        ColorStateList.valueOf(color));
                holder.color = color;

                if (!ColorUtils.isColorDark(color)) {
                    holder.message.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.darkText));
                } else {
                    holder.message.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.lightText));
                }
            } else if (fromColorMapperByName != null && fromColorMapperByName.containsKey(message.from)) {
                // group convo, color them differently
                // this is the usual result
                int color = fromColorMapperByName.get(message.from).colors.color;
                holder.messageHolder.setBackgroundTintList(
                        ColorStateList.valueOf(color));
                holder.color = color;

                if (!ColorUtils.isColorDark(color)) {
                    holder.message.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.darkText));
                } else {
                    holder.message.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.lightText));
                }
            } else {
                // group convo without the contact here.. uh oh. Could happen if the conversation
                // title doesn't match the message.from database column.
                final Contact contact = new Contact();
                contact.name = message.from;
                contact.phoneNumber = message.from;
                contact.colors = ColorUtils.getRandomMaterialColor(holder.itemView.getContext());

                if (fromColorMapper == null) {
                    fromColorMapper = new HashMap<>();
                }

                fromColorMapper.put(message.from, contact);

                // then write it to the database for later
                new Thread(() -> {
                    DataSource source = DataSource.getInstance(holder.itemView.getContext());
                    source.open();
                    source.insertContact(contact);
                    source.close();
                }).start();
            }
        }
    }
}
