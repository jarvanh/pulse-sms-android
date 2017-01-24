package xyz.klinker.messenger.util;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Message;

public class MessageListStylingHelper {

    private boolean roundMessages;
    private int eightDp;

    private int columnType = -1;
    private int columnTimestamp = -1;

    private boolean isJustSentMessage;
    private int currentType;
    private long currentTimestamp;
    private int lastType;
    private long lastTimestamp;
    private int nextType;
    private long nextTimestamp;

    public MessageListStylingHelper(Context context) {
        try {
            eightDp = DensityUtil.toDp(context, 8);
            roundMessages = Settings.get(context).rounderBubbles;
        } catch (NullPointerException e) {
            eightDp = 8;
            roundMessages = false;
        }
    }

    public MessageListStylingHelper calculateAdjacentItems(Cursor cursor, int currentPosition) {
        if (columnTimestamp == -1) {
            columnTimestamp = cursor.getColumnIndex(Message.COLUMN_TIMESTAMP);
        }
        if (columnType == -1) {
            columnType = cursor.getColumnIndex(Message.COLUMN_TYPE);
        }

        cursor.moveToPosition(currentPosition);
        currentType = cursor.getInt(columnType);
        currentTimestamp = cursor.getLong(columnTimestamp);

        if (currentPosition > 0) {
            cursor.moveToPosition(currentPosition - 1);
            lastType = cursor.getInt(columnType);
            lastTimestamp = cursor.getLong(columnTimestamp);
        } else {
            lastType = -1;
            lastTimestamp = -1;
        }

        if (currentPosition != cursor.getCount() - 1) {
            cursor.moveToPosition(currentPosition + 1);
            nextType = cursor.getInt(columnType);
            nextTimestamp = cursor.getLong(columnTimestamp);
            isJustSentMessage = false;
        } else {
            isJustSentMessage = currentType != Message.TYPE_RECEIVED;
            nextType = -1;
            nextTimestamp = System.currentTimeMillis();
        }

        return this;
    }

    public MessageListStylingHelper setMargins(MessageViewHolder holder) {
        if (holder.itemView.getLayoutParams() == null) {
            return this;
        }

        if (currentType != lastType) {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).topMargin = eightDp;
        } else {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).topMargin = 0;
        }

        if (!isJustSentMessage && (currentType != nextType || TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp))) {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).bottomMargin = eightDp;
        } else {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).bottomMargin = 0;
        }

        return this;
    }

    public MessageListStylingHelper setBackground(MessageViewHolder holder) {
        if (roundMessages || MimeType.isExpandedMedia(holder.mimeType) || currentType == Message.TYPE_INFO || holder.messageHolder == null) {
            return this;
        }

        if (currentType == lastType && !TimeUtils.shouldDisplayTimestamp(lastTimestamp, currentTimestamp)) {
            if (currentType == Message.TYPE_RECEIVED) {
                holder.messageHolder.setBackground(holder.itemView.getContext().getResources().getDrawable(R.drawable.message_received_group_background));
            } else {
                holder.messageHolder.setBackground(holder.itemView.getContext().getResources().getDrawable(R.drawable.message_sent_group_background));
            }
        } else {
            if (currentType == Message.TYPE_RECEIVED) {
                holder.messageHolder.setBackground(holder.itemView.getContext().getResources().getDrawable(R.drawable.message_received_background));
            } else {
                holder.messageHolder.setBackground(holder.itemView.getContext().getResources().getDrawable(R.drawable.message_sent_background));
            }
        }

        return this;
    }

    public MessageListStylingHelper applyTimestampHeight(MessageViewHolder holder, int timestampHeight) {
        if (TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp)) {
            holder.timestamp.getLayoutParams().height = timestampHeight;
        } else {
            holder.timestamp.getLayoutParams().height = 0;
        }

        return this;
    }
}
