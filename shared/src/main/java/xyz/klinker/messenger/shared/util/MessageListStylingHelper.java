package xyz.klinker.messenger.shared.util;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Message;

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
            columnTimestamp = cursor.getColumnIndex(Message.Companion.getCOLUMN_TIMESTAMP());
        }
        if (columnType == -1) {
            columnType = cursor.getColumnIndex(Message.Companion.getCOLUMN_TYPE());
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
            isJustSentMessage = currentType != Message.Companion.getTYPE_RECEIVED();
            nextType = -1;
            nextTimestamp = System.currentTimeMillis();
        }

        return this;
    }

    public MessageListStylingHelper setMargins(View itemView) {
        if (itemView.getLayoutParams() == null) {
            return this;
        }

        if (currentType != lastType) {
            ((RecyclerView.LayoutParams) itemView.getLayoutParams()).topMargin = eightDp;
        } else {
            ((RecyclerView.LayoutParams) itemView.getLayoutParams()).topMargin = 0;
        }

        if (!isJustSentMessage && (currentType != nextType || TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp))) {
            ((RecyclerView.LayoutParams) itemView.getLayoutParams()).bottomMargin = eightDp;
        } else {
            ((RecyclerView.LayoutParams) itemView.getLayoutParams()).bottomMargin = 0;
        }

        return this;
    }

    public MessageListStylingHelper setBackground(View messageHolder, String mimeType) {
        if (MimeType.INSTANCE.isExpandedMedia(mimeType) || currentType == Message.Companion.getTYPE_INFO() || messageHolder == null) {

        } else if (mimeType.contains("image") || mimeType.contains("video")) {
            messageHolder.setBackground(null);
        } else {
            int background;
            if (roundMessages) {
                background = roundBubbleBackground();
            } else {
                background = dialogSquareBackground();
            }

            messageHolder.setBackground(
                    messageHolder.getContext().getResources().getDrawable(background)
            );
        }

        return this;
    }

    public MessageListStylingHelper applyTimestampHeight(TextView timestamp, int timestampHeight) {
        if (TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp)) {
            timestamp.getLayoutParams().height = timestampHeight;
        } else {
            timestamp.getLayoutParams().height = 0;
        }

        return this;
    }

    @DrawableRes
    private int dialogSquareBackground() {
        if (currentType == lastType && !TimeUtils.shouldDisplayTimestamp(lastTimestamp, currentTimestamp)) {
            if (currentType == Message.Companion.getTYPE_RECEIVED()) {
                return R.drawable.message_received_group_background;
            } else {
                return R.drawable.message_sent_group_background;
            }
        } else {
            if (currentType == Message.Companion.getTYPE_RECEIVED()) {
                return R.drawable.message_received_background;
            } else {
                return R.drawable.message_sent_background;
            }
        }
    }

    @DrawableRes
    private int roundBubbleBackground() {
        boolean displayNextTimestamp = TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp);
        boolean displayLastTimestamp = TimeUtils.shouldDisplayTimestamp(lastTimestamp, currentTimestamp);

        if (currentType == lastType && currentType == nextType && !displayLastTimestamp && !displayNextTimestamp) {
            // both corners
            if (currentType == Message.Companion.getTYPE_RECEIVED()) {
                return R.drawable.message_circle_received_group_both_background;
            } else {
                return R.drawable.message_circle_sent_group_both_background;
            }
        } else if ((currentType == lastType && currentType != nextType && !displayLastTimestamp) ||
                (currentType == nextType && currentType == lastType && displayNextTimestamp)) {
            // top corner bubble
            if (currentType == Message.Companion.getTYPE_RECEIVED()) {
                return R.drawable.message_circle_received_group_top_background;
            } else {
                return R.drawable.message_circle_sent_group_top_background;
            }
        } else if ((currentType == nextType && currentType != lastType && !displayNextTimestamp) ||
                (currentType == nextType && currentType == lastType && displayLastTimestamp)) {
            // bottom corner bubble
            if (currentType == Message.Companion.getTYPE_RECEIVED()) {
                return R.drawable.message_circle_received_group_bottom_background;
            } else {
                return R.drawable.message_circle_sent_group_bottom_background;
            }
        } else {
            return R.drawable.message_circle_background;
        }
    }
}
