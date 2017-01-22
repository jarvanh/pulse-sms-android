package xyz.klinker.messenger.util;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;

import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder;
import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.data.model.Message;

public class MessageListStylingHelper {

    private boolean paddingFlag;
    private int eightDp;

    private int columnType = -1;
    private int columnTimestamp = -1;

    private int currentType;
    private long currentTimestamp;

    private int lastType;
    private int nextType;
    private long nextTimestamp;

    public MessageListStylingHelper(Context context) {
        eightDp = DensityUtil.toDp(context, 8);
        paddingFlag = FeatureFlags.get(context).MESSAGE_PADDING;
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
        } else {
            lastType = -1;
        }

        if (currentPosition != cursor.getCount() - 1) {
            cursor.moveToPosition(currentPosition + 1);
            nextType = cursor.getInt(columnType);
            nextTimestamp = cursor.getLong(columnTimestamp);
        } else {
            nextType = -1;
            nextTimestamp = System.currentTimeMillis();
        }

        return this;
    }

    public MessageListStylingHelper setMargins(MessageViewHolder holder) {
        if (!paddingFlag) {
            return this;
        }

        if (currentType != lastType) {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).topMargin = eightDp * 4;
        } else {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).topMargin = 0;
        }

        if (currentType != nextType || TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp)) {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).bottomMargin = eightDp * 2;
        } else {
            ((RecyclerView.LayoutParams) holder.itemView.getLayoutParams()).bottomMargin = 0;
        }

        return this;
    }

    public MessageListStylingHelper setBackground(MessageViewHolder holder) {
        if (!paddingFlag) {
            return this;
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
