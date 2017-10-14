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
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.WearableMessageViewHolder;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.shared.data.ArticlePreview;
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

        Account account = Account.INSTANCE;
        ignoreSendingStatus = true;

        if (context == null) {
            imageHeight = imageWidth = 50;
        } else {
            imageHeight = imageWidth = DensityUtil.INSTANCE.toPx(context, 100);
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
            setTimestampHeight(DensityUtil.INSTANCE.spToPx(parent.getContext(),
                    Settings.INSTANCE.getMediumFont() + 2));
        }

        boolean rounder = Settings.INSTANCE.getRounderBubbles();
        if (viewType == Message.Companion.getTYPE_RECEIVED()) {
            layoutId = rounder ? R.layout.message_received_round : R.layout.message_received;
            color = receivedColor;
        } else {
            color = -1;

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
            } else {
                layoutId = rounder ? R.layout.message_sent_round : R.layout.message_sent;
            }
        }

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);

        messages.moveToFirst();
        WearableMessageViewHolder holder = new WearableMessageViewHolder(view, color,
                messages.getLong(messages.getColumnIndex(Message.Companion.getCOLUMN_CONVERSATION_ID())),
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

        holder.messageId = message.getId();
        holder.mimeType = message.getMimeType();
        holder.data = message.getData();

        if (message.getMimeType().equals(MimeType.INSTANCE.getTEXT_PLAIN())) {
            holder.message.setText(message.getData());

            setGone(holder.image);
            setVisible(holder.message);
        } else if (!MimeType.INSTANCE.isExpandedMedia(message.getMimeType())) {
            holder.image.setImageDrawable(null);

            if (MimeType.INSTANCE.isStaticImage(message.getMimeType())) {
                holder.image.setImageResource(getItemViewType(position) != Message.Companion.getTYPE_RECEIVED() ?
                    R.drawable.ic_image_sending : R.drawable.ic_image);
            } else if (message.getMimeType().equals(MimeType.INSTANCE.getIMAGE_GIF())) {
                holder.image.setImageResource(getItemViewType(position) != Message.Companion.getTYPE_RECEIVED() ?
                        R.drawable.ic_image_sending : R.drawable.ic_image);
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
                                .fitCenter())
                        .into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                                ImageUtils.INSTANCE.overlayBitmap(holder.image.getContext(),
                                        bitmap, R.drawable.ic_play);
                                holder.image.setImageBitmap(bitmap);
                            }
                        });
            } else if (MimeType.INSTANCE.isAudio(message.getMimeType())) {
                holder.image.setImageResource(getItemViewType(position) != Message.Companion.getTYPE_RECEIVED() ?
                        R.drawable.ic_audio_sent : R.drawable.ic_audio);
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
                    Glide.with(holder.clippedImage.getContext())
                            .asBitmap()
                            .load(Uri.parse(preview.getThumbnail()))
                            .apply(new RequestOptions()
                                    .override(holder.image.getMaxHeight(), holder.image.getMaxHeight())
                                    .fitCenter())
                            .into(new SimpleTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                                    ImageUtils.INSTANCE.overlayBitmap(holder.image.getContext(),
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
                            .load(Uri.parse(message.getData()))
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
        } else {
            holder.timestamp.setText(TimeUtils.INSTANCE.formatTimestamp(holder.timestamp.getContext(),
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
        int type = messages.getInt(messages.getColumnIndex(Message.Companion.getCOLUMN_TYPE()));
        if (ignoreSendingStatus && type == Message.Companion.getTYPE_SENDING()) {
            type = Message.Companion.getTYPE_SENT();
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
        return messages.getLong(messages.getColumnIndex(Message.Companion.getCOLUMN_ID()));
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