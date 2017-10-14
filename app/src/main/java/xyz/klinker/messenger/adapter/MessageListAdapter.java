/*
 * Copyright (C) 2017 Luke Klinker
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
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.klinker.android.link_builder.Link;
import com.klinker.android.link_builder.LinkBuilder;
import com.klinker.android.link_builder.TouchableMovementMethod;
import com.turingtechnologies.materialscrollbar.IDateableAdapter;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private int receivedLinkColor;
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
            imageHeight = imageWidth = DensityUtil.INSTANCE.toPx(fragment.getActivity(), 350);
        }

        try {
            ignoreSendingStatus = Account.INSTANCE.exists() && !Account.INSTANCE.getPrimary();
        } catch (Exception e) {
            ignoreSendingStatus = false;
        }

        if (Build.FINGERPRINT.equals("robolectric") || FeatureFlags.INSTANCE.getREENABLE_SENDING_STATUS_ON_NON_PRIMARY()) {
            ignoreSendingStatus = false;
        }

        stylingHelper = new MessageListStylingHelper(fragment.getActivity());

        if (fragment.getMultiSelect() != null)
            fragment.getMultiSelect().setAdapter(this);

        largeFont = Settings.INSTANCE.getLargeFont();
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId;
        int color;

        if (timestampHeight == 0) {
            setTimestampHeight(DensityUtil.INSTANCE.spToPx(parent.getContext(),
                    Settings.INSTANCE.getMediumFont() + 2));
        }

        boolean rounder = Settings.INSTANCE.getRounderBubbles();
        if (viewType == Message.Companion.getTYPE_RECEIVED()) {
            layoutId = rounder ? R.layout.message_received_round : R.layout.message_received;
            color = receivedColor;
        } else {
            color = Integer.MIN_VALUE;

            if (viewType == Message.Companion.getTYPE_SENDING()) {
                layoutId = rounder ? R.layout.message_sending_round : R.layout.message_sending;
            } else if (viewType == Message.Companion.getTYPE_ERROR()) {
                layoutId = rounder ? R.layout.message_error_round : R.layout.message_error;
            } else if (viewType == Message.Companion.getTYPE_DELIVERED()) {
                layoutId = rounder ? R.layout.message_delivered_round : R.layout.message_delivered;
            } else if (viewType == Message.Companion.getTYPE_INFO()) {
                layoutId = R.layout.message_info;
            } else if (viewType == Message.Companion.getTYPE_MEDIA()) {
                layoutId = R.layout.message_media;
            } else if (viewType == Message.Companion.getTYPE_IMAGE_SENDING()) {
                layoutId = rounder ? R.layout.message_image_sending_round : R.layout.message_image_sending;
            } else if (viewType == Message.Companion.getTYPE_IMAGE_SENT()) {
                layoutId = rounder ? R.layout.message_image_sent_round : R.layout.message_image_sent;
            } else if (viewType == Message.Companion.getTYPE_IMAGE_RECEIVED()) {
                layoutId = rounder ? R.layout.message_image_received_round : R.layout.message_image_received;
            } else {
                layoutId = rounder ? R.layout.message_sent_round : R.layout.message_sent;
            }
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        messages.moveToFirst();
        MessageViewHolder holder = new MessageViewHolder(fragment, view, fromColorMapper != null && fromColorMapper.size() > 1 ? Integer.MIN_VALUE : color,
                messages.getLong(messages.getColumnIndex(Message.Companion.getCOLUMN_CONVERSATION_ID())),
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

        holder.messageId = message.getId();
        holder.mimeType = message.getMimeType();
        holder.data = message.getData();

        int backgroundColor = colorMessage(holder, message);

        if (message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN())) {
            holder.message.setText(message.getData());

            if (!message.getData().isEmpty() && message.getData().replaceAll(Regex.INSTANCE.getEMOJI(), "").isEmpty()) {
                // enlarge emojis
                holder.message.setTextSize(35);
            } else {
                holder.message.setTextSize(largeFont);
            }

            int linkColor = accentColor;
            if (message.getType() == Message.Companion.getTYPE_RECEIVED()) {
                if (ColorUtils.INSTANCE.isColorDark(backgroundColor != Integer.MIN_VALUE ? backgroundColor : receivedColor)) {
                    linkColor = holder.itemView.getContext().getResources().getColor(R.color.lightText);
                } else {
                    linkColor = holder.itemView.getContext().getResources().getColor(R.color.darkText);
                }
            }

            Link urls = new Link(Regex.INSTANCE.getWEB_URL());
            urls.setTextColor(linkColor);
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

                if (clickedText.contains("youtube") || !Settings.INSTANCE.getInternalBrowser()) {
                    Intent url = new Intent(Intent.ACTION_VIEW);
                    url.setData(Uri.parse(clickedText));
                    holder.itemView.getContext().startActivity(url);
                } else {
                    ArticleIntent intent = new ArticleIntent.Builder(holder.itemView.getContext(), ArticleParser.Companion.getARTICLE_API_KEY())
                            .setToolbarColor(receivedColor)
                            .setAccentColor(accentColor)
                            .setTheme(Settings.INSTANCE.isCurrentlyDarkTheme() ?
                                    ArticleIntent.THEME_DARK : ArticleIntent.THEME_LIGHT)
                            .setTextSize(Settings.INSTANCE.getMediumFont() + 1)
                            .build();

                    intent.launchUrl(holder.itemView.getContext(), Uri.parse(clickedText));
                }
            });

            Link phoneNumbers = new Link(Regex.INSTANCE.getPHONE());
            phoneNumbers.setTextColor(linkColor);
            phoneNumbers.setHighlightAlpha(.4f);
            phoneNumbers.setOnClickListener(clickedText -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + PhoneNumberUtils.clearFormatting(clickedText)));
                holder.message.getContext().startActivity(intent);
            });

            Link emails = new Link(Patterns.EMAIL_ADDRESS);
            emails.setTextColor(linkColor);
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
        } else if (!MimeType.INSTANCE.isExpandedMedia(message.getMimeType())) {
            holder.image.setImageDrawable(null);
            holder.image.setMinimumWidth(imageWidth);
            holder.image.setMinimumHeight(imageHeight);

            if (MimeType.INSTANCE.isStaticImage(message.getMimeType())) {
                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.getData()))
                        .apply(new RequestOptions()
                            .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                            .fitCenter())
                        .into(holder.image);
            } else if (message.getMimeType().equals(MimeType.INSTANCE.getIMAGE_GIF())) {
                holder.image.setMaxWidth(holder.image.getContext()
                        .getResources().getDimensionPixelSize(R.dimen.max_gif_width));
                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.getData()))
                        .apply(new RequestOptions()
                                .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                                .diskCacheStrategy(DiskCacheStrategy.DATA)
                                .fitCenter())
                        .into(holder.image);
            } else if (MimeType.INSTANCE.isVideo(message.getMimeType())) {
                Drawable placeholder;
                if (getItemViewType(position) != Message.Companion.getTYPE_RECEIVED()) {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_play_sent);
                } else {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_play);
                }

                Glide.with(holder.image.getContext())
                        .asBitmap()
                        .load(Uri.parse(message.getData()))
                        .apply(new RequestOptions()
                                .error(placeholder)
                                .placeholder(placeholder)
                                .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                                .diskCacheStrategy(DiskCacheStrategy.DATA)
                                .fitCenter())
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                                ImageUtils.overlayBitmap(holder.image.getContext(),
                                        resource, R.drawable.ic_play);
                                holder.image.setImageBitmap(resource);
                            }
                        });
            } else if (MimeType.INSTANCE.isAudio(message.getMimeType())) {
                Drawable placeholder;
                if (getItemViewType(position) != Message.Companion.getTYPE_RECEIVED()) {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_audio_sent);
                } else {
                    placeholder = holder.image.getContext()
                            .getDrawable(R.drawable.ic_audio);
                }

                Glide.with(holder.image.getContext())
                        .load(Uri.parse(message.getData()))
                        .apply(new RequestOptions()
                                .error(placeholder)
                                .placeholder(placeholder))
                        .into(holder.image);
            } else if (MimeType.INSTANCE.isVcard(message.getMimeType())) {
                holder.message.setText(message.getData());
                holder.image.setImageResource(getItemViewType(position) != Message.Companion.getTYPE_RECEIVED() ?
                        R.drawable.ic_contacts_sent : R.drawable.ic_contacts);
            } else {
                Log.v("MessageListAdapter", "unused mime type: " + message.getMimeType());
            }

            setGone(holder.message);
            setVisible(holder.image);
        } else {
            if (message.getMimeType().equals(MimeType.INSTANCE.getMEDIA_YOUTUBE_V2())) {
                YouTubePreview preview = YouTubePreview.Companion.build(message.getData());
                if (preview != null) {
                    Glide.with(holder.image.getContext())
                            .asBitmap()
                            .load(Uri.parse(preview.getThumbnail()))
                            .apply(new RequestOptions()
                                    .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                                    .fitCenter())
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                                    ImageUtils.overlayBitmap(holder.image.getContext(),
                                            bitmap, R.drawable.ic_play);
                                    holder.clippedImage.setImageBitmap(bitmap);
                                }
                            });

                    holder.contact.setText(preview.getTitle());
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
            } else if (message.getMimeType().equals(MimeType.INSTANCE.getMEDIA_TWITTER())) {

            } else if (message.getMimeType().equals(MimeType.INSTANCE.getMEDIA_ARTICLE())) {
                ArticlePreview preview = ArticlePreview.Companion.build(message.getData());
                if (preview != null) {
                    Glide.with(holder.clippedImage.getContext())
                            .asBitmap()
                            .load(Uri.parse(preview.getImageUrl()))
                            .apply(new RequestOptions()
                                    .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                                    .fitCenter())
                            .into(holder.clippedImage);

                    holder.contact.setText(preview.getTitle());
                    holder.message.setText(preview.getDescription());
                    holder.title.setText(preview.getDomain());

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

        if (message.getSimPhoneNumber() != null) {
            holder.timestamp.setText(TimeUtils.INSTANCE.formatTimestamp(holder.timestamp.getContext(),
                    message.getTimestamp()) + " (SIM " + message.getSimPhoneNumber() + ")");
        } else if (holder.timestamp != null) {
            holder.timestamp.setText(TimeUtils.INSTANCE.formatTimestamp(holder.itemView.getContext(),
                    message.getTimestamp()));
        }

        if (!isGroup) {
            stylingHelper.calculateAdjacentItems(messages, position)
                    .setMargins(holder.itemView)
                    .setBackground(holder.messageHolder, message.getMimeType())
                    .applyTimestampHeight(holder.timestamp, timestampHeight);
        } else {
            stylingHelper.calculateAdjacentItems(messages, position)
                    .applyTimestampHeight(holder.timestamp, timestampHeight);
        }

        if (isGroup && holder.contact != null && message.getFrom() != null) {
            if (holder.contact.getVisibility() == View.GONE) {
                holder.contact.setVisibility(View.VISIBLE);
            }

            int label = holder.timestamp.getLayoutParams().height > 0 ?
                    R.string.message_from_bullet : R.string.message_from;
            holder.contact.setText(holder.contact.getResources().getString(label, message.getFrom()));
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
        try {
            messages.moveToPosition(position);
            int type = messages.getInt(messages.getColumnIndex(Message.Companion.getCOLUMN_TYPE()));
            String mimeType = messages.getString(messages.getColumnIndex(Message.Companion.getCOLUMN_MIME_TYPE()));

            if (ignoreSendingStatus && type == Message.Companion.getTYPE_SENDING()) {
                if (mimeType != null && (mimeType.contains("image") || mimeType.contains("video"))) {
                    type = Message.Companion.getTYPE_IMAGE_SENT();
                } else {
                    type = Message.Companion.getTYPE_SENT();
                }
            } else if (mimeType != null && (mimeType.contains("image") || mimeType.contains("video"))) {
                if (type == Message.Companion.getTYPE_RECEIVED()) {
                    type = Message.Companion.getTYPE_IMAGE_RECEIVED();
                } else if (type == Message.Companion.getTYPE_SENDING()) {
                    type = Message.Companion.getTYPE_IMAGE_SENDING();
                } else {
                    type = Message.Companion.getTYPE_IMAGE_SENT();
                }
            }

            return type;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public Date getDateForElement(int position) {
        if (position < 0) {
            return new Date();
        }

        messages.moveToPosition(position);
        long millis = messages.getLong(messages.getColumnIndex(Message.Companion.getCOLUMN_TIMESTAMP()));
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
        try {
            messages.moveToPosition(position);
            return messages.getLong(messages.getColumnIndex(Message.Companion.getCOLUMN_ID()));
        } catch (Exception e) {
            return -1L;
        }
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

            Log.v("MessageListAdapter", "initial count: " + initialCount + ", final count: " + finalCount);

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
                } else if (recycler != null && messages.moveToLast()) {
                    Message message = new Message();
                    message.fillFromCursor(messages);
                    if (message.getType() == Message.Companion.getTYPE_RECEIVED()) {
                        String text = recycler.getContext().getString(R.string.new_message);
                        snackbar = Snackbar
                                .make(recycler, text, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.read, view -> manager.scrollToPosition(finalCount - 1));

                        try {
                            ((CoordinatorLayout.LayoutParams) snackbar.getView().getLayoutParams())
                                    .bottomMargin = DensityUtil.INSTANCE.toDp(recycler.getContext(), 56);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        SnackbarAnimationFix.INSTANCE.apply(snackbar);
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
        DataSource source = DataSource.INSTANCE;

        List<Message> messageList = source.getMessages(context, conversationId, 1);
        if (messageList.size() == 0) {
            ((MessengerActivity) fragment.getActivity()).menuItemClicked(R.id.menu_delete_conversation);
        } else {
            Message message = messageList.get(0);

            Conversation conversation = source.getConversation(context, conversationId);
            source.updateConversation(context, conversationId, true, message.getTimestamp(),
                    message.getType() == Message.Companion.getTYPE_SENT() || message.getType() == Message.Companion.getTYPE_SENDING() ?
                            context.getString(R.string.you) + ": " + message.getData() : message.getData(),
                    message.getMimeType(), conversation != null && conversation.getArchive());

            fragment.setConversationUpdateInfo(message.getType() == Message.Companion.getTYPE_SENDING() ?
                    context.getString(R.string.you) + ": " + message.getData() : message.getData());
        }
    }

    public void setFromColorMapper(Map<String, Contact> colorMapper, Map<String, Contact> colorMapperByName) {
        this.fromColorMapper = colorMapper;
        this.fromColorMapperByName = colorMapperByName;
    }

    private int colorMessage(final MessageViewHolder holder, final Message message) {
        if (Settings.INSTANCE.getUseGlobalThemeColor()) {
            return Integer.MIN_VALUE;
        }

        if (message.getType() == Message.Companion.getTYPE_RECEIVED() &&
                fromColorMapper != null && fromColorMapper.size() > 1) { // size > 1 so we know it is a group convo
            if (fromColorMapper.containsKey(message.getFrom())) {
                // group convo, color them differently
                // this is the usual result
                int color = fromColorMapper.get(message.getFrom()).getColors().getColor();
                holder.messageHolder.setBackgroundTintList(
                        ColorStateList.valueOf(color));
                holder.color = color;

                if (holder.message != null) {
                    if (!ColorUtils.INSTANCE.isColorDark(color)) {
                        holder.message.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.darkText));
                    } else {
                        holder.message.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.lightText));
                    }
                }

                return color;
            } else if (fromColorMapperByName != null && fromColorMapperByName.containsKey(message.getFrom())) {
                // group convo, color them differently
                // this is the usual result
                int color = fromColorMapperByName.get(message.getFrom()).getColors().getColor();
                holder.messageHolder.setBackgroundTintList(
                        ColorStateList.valueOf(color));
                holder.color = color;

                if (holder.message != null) {
                    if (!ColorUtils.INSTANCE.isColorDark(color)) {
                        holder.message.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.darkText));
                    } else {
                        holder.message.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.lightText));
                    }
                }

                return color;
            } else {
                // group convo without the contact here.. uh oh. Could happen if the conversation
                // title doesn't match the message from database column.
                final Contact contact = new Contact();
                contact.setName(message.getFrom());
                contact.setPhoneNumber(message.getFrom());
                contact.setColors(ColorUtils.INSTANCE.getRandomMaterialColor(holder.itemView.getContext()));

                if (fromColorMapper == null) {
                    fromColorMapper = new HashMap<>();
                }

                fromColorMapper.put(message.getFrom(), contact);

                // then write it to the database for later
                new Thread(() -> {
                    final Context context = holder.itemView.getContext();
                    DataSource source = DataSource.INSTANCE;

                    if (contact.getPhoneNumber() != null) {
                        int originalLength = contact.getPhoneNumber().length();
                        int newLength = contact.getPhoneNumber().replaceAll("[0-9]", "").length();
                        if (originalLength == newLength) {
                            // all letters, so we should use the contact name to find the phone number
                            List<Contact> contacts = source.getContactsByNames(context, contact.getName());
                            if (contacts.size() > 0) {
                                contact.setPhoneNumber(contacts.get(0).getPhoneNumber());
                            }
                        }

                        source.insertContact(context, contact);
                    }
                }).start();

                return contact.getColors().getColor();
            }
        }

        return Integer.MIN_VALUE;
    }
}
