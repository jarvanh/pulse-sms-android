package xyz.klinker.messenger.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.WearableMessageViewHolder;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.ArticlePreview;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.YouTubePreview;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.util.DensityUtil;
import xyz.klinker.messenger.shared.util.ImageUtils;
import xyz.klinker.messenger.shared.util.MessageListStylingHelper;
import xyz.klinker.messenger.shared.util.TimeUtils;

public class WearableMessageListAdapter extends RecyclerView.Adapter<WearableMessageViewHolder> {

    private static final String TAG = "MessageListAdapter";

    private Cursor messages;
    private boolean isGroup;
    private boolean ignoreSendingStatus;
    private int receivedColor;
    private int accentColor;
    private int timestampHeight;

    private LinearLayoutManager manager;
    private MessageListStylingHelper stylingHelper;

    private int imageHeight;
    private int imageWidth;

    public WearableMessageListAdapter(Context context, LinearLayoutManager manager, Cursor messages, int receivedColor, int accentColor, boolean isGroup) {
        this.messages = messages;
        this.isGroup = isGroup;
        this.receivedColor = receivedColor;
        this.accentColor = accentColor;
        this.timestampHeight = 0;

        this.manager = manager;
        this.stylingHelper = new MessageListStylingHelper(context);

        Account account = Account.get(context);
        ignoreSendingStatus = account.exists() && !account.primary;

        if (context == null) {
            imageHeight = imageWidth = 50;
        } else {
            imageHeight = imageWidth = DensityUtil.toPx(context, 100);
        }
    }

    @Override
    public WearableMessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == -1) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_footer, parent, false);
            return new WearableMessageViewHolder(view, -1, -1, -1, -1);
        }

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
        WearableMessageViewHolder holder = new WearableMessageViewHolder(view, color,
                messages.getLong(messages.getColumnIndex(Message.COLUMN_CONVERSATION_ID)),
                viewType, timestampHeight);

        holder.setColors(receivedColor, accentColor);

        return holder;
    }

    @VisibleForTesting
    void setTimestampHeight(int height) {
        this.timestampHeight = height;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(final WearableMessageViewHolder holder, int position) {
        if (position == getItemCount() - 1) {
            return;
        }

        messages.moveToPosition(position);
        Message message = new Message();
        message.fillFromCursor(messages);

        holder.messageId = message.id;
        holder.mimeType = message.mimeType;
        holder.data = message.data;

        if (message.mimeType.equals(MimeType.TEXT_PLAIN)) {
            holder.message.setText(message.data);

            setGone(holder.image);
            setVisible(holder.message);
        } else if (!MimeType.isExpandedMedia(message.mimeType)) {
            holder.image.setImageDrawable(null);

            if (MimeType.isStaticImage(message.mimeType)) {
                holder.image.setImageResource(getItemViewType(position) != Message.TYPE_RECEIVED ?
                    R.drawable.ic_image_sending : R.drawable.ic_image);
            } else if (message.mimeType.equals(MimeType.IMAGE_GIF)) {
                holder.image.setImageResource(getItemViewType(position) != Message.TYPE_RECEIVED ?
                        R.drawable.ic_image_sending : R.drawable.ic_image);
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
                holder.image.setImageResource(getItemViewType(position) != Message.TYPE_RECEIVED ?
                        R.drawable.ic_audio_sent : R.drawable.ic_audio);
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
            return messages.getCount() + 1;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (messages == null || messages.getCount() == 0 || position == getItemCount() - 1) {
            return -1;
        }

        messages.moveToPosition(position);
        int type = messages.getInt(messages.getColumnIndex(Message.COLUMN_TYPE));
        if (ignoreSendingStatus && type == Message.TYPE_SENDING) {
            type = Message.TYPE_SENT;
        }

        return type;
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

    public void setMessages(Cursor cursor) {
        this.messages = cursor;
        notifyDataSetChanged();
    }

    public void addMessage(Cursor newMessages) {
        int initialCount = getItemCount();

        try {
            messages.close();
        } catch (Exception e) { }

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
                }
            }
        }
    }

    public Cursor getMessages() {
        return messages;
    }

}