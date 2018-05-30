package xyz.klinker.messenger.shared.util

import android.content.Context
import android.database.Cursor
import android.support.annotation.DrawableRes
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.pojo.BubbleTheme

class MessageListStylingHelper(context: Context?) {

    private val eightDp: Int = DensityUtil.toDp(context, 8)

    private var columnType = -1
    private var columnTimestamp = -1

    private var currentType: Int = 0
    private var currentTimestamp: Long = 0
    private var lastType: Int = 0
    private var lastTimestamp: Long = 0
    private var nextType: Int = 0
    private var nextTimestamp: Long = 0


    fun calculateAdjacentItems(cursor: Cursor, currentPosition: Int): MessageListStylingHelper {
        if (columnTimestamp == -1) {
            columnTimestamp = cursor.getColumnIndex(Message.COLUMN_TIMESTAMP)
        }

        if (columnType == -1) {
            columnType = cursor.getColumnIndex(Message.COLUMN_TYPE)
        }

        cursor.moveToPosition(currentPosition)
        currentType = cursor.getInt(columnType)
        currentTimestamp = cursor.getLong(columnTimestamp)

        if (currentPosition > 0) {
            cursor.moveToPosition(currentPosition - 1)
            lastType = cursor.getInt(columnType)
            lastTimestamp = cursor.getLong(columnTimestamp)
        } else {
            lastType = -1
            lastTimestamp = -1
        }

        if (currentPosition != cursor.count - 1) {
            cursor.moveToPosition(currentPosition + 1)
            nextType = cursor.getInt(columnType)
            nextTimestamp = cursor.getLong(columnTimestamp)
        } else {
            nextType = -1
            nextTimestamp = TimeUtils.now
        }

        return this
    }

    fun setMargins(itemView: View): MessageListStylingHelper {
        if (itemView.layoutParams == null) {
            return this
        }

        if (!sameType(currentType, lastType)) {
            (itemView.layoutParams as RecyclerView.LayoutParams).topMargin = eightDp
        } else {
            (itemView.layoutParams as RecyclerView.LayoutParams).topMargin = 0
        }

        if (!sameType(currentType, nextType) || TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp)) {
            (itemView.layoutParams as RecyclerView.LayoutParams).bottomMargin = eightDp
        } else {
            (itemView.layoutParams as RecyclerView.LayoutParams).bottomMargin = 0
        }

        return this
    }

    fun setBackground(messageHolder: View?, mimeType: String): MessageListStylingHelper {
        if (MimeType.isExpandedMedia(mimeType) || currentType == Message.TYPE_INFO || messageHolder == null) {

        } else if (mimeType.contains("image") || mimeType.contains("video")) {
            messageHolder.background = null
        } else {
            val background = when (Settings.bubbleTheme) {
                BubbleTheme.MATERIAL -> materialThemeStyleBackground()
                BubbleTheme.ROUND -> roundBubbleBackground()
                BubbleTheme.CLASSIC -> dialogSquareBackground()
            }

            messageHolder.background = messageHolder.context.resources.getDrawable(background)
        }

        return this
    }

    fun applyTimestampHeight(timestamp: TextView, timestampHeight: Int): MessageListStylingHelper {
        if (TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp)) {
            timestamp.layoutParams.height = timestampHeight
        } else {
            timestamp.layoutParams.height = 0
        }

        return this
    }

    @DrawableRes
    private fun dialogSquareBackground(): Int {
        return if (currentType == lastType && !TimeUtils.shouldDisplayTimestamp(lastTimestamp, currentTimestamp)) {
            if (currentType == Message.TYPE_RECEIVED) {
                R.drawable.message_received_group_background
            } else {
                R.drawable.message_sent_group_background
            }
        } else {
            if (currentType == Message.TYPE_RECEIVED) {
                R.drawable.message_received_background
            } else {
                R.drawable.message_sent_background
            }
        }
    }

    @DrawableRes
    private fun roundBubbleBackground(): Int {
        val displayNextTimestamp = TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp)
        val displayLastTimestamp = TimeUtils.shouldDisplayTimestamp(lastTimestamp, currentTimestamp)

        return if (currentType == lastType && currentType == nextType && !displayLastTimestamp && !displayNextTimestamp) {
            // both corners
            if (currentType == Message.TYPE_RECEIVED) {
                R.drawable.message_circle_received_group_both_background
            } else {
                R.drawable.message_circle_sent_group_both_background
            }
        } else if (currentType == lastType && currentType != nextType && !displayLastTimestamp || currentType == nextType && currentType == lastType && displayNextTimestamp) {
            // top corner bubble
            if (currentType == Message.TYPE_RECEIVED) {
                R.drawable.message_circle_received_group_top_background
            } else {
                R.drawable.message_circle_sent_group_top_background
            }
        } else if (currentType == nextType && currentType != lastType && !displayNextTimestamp || currentType == nextType && currentType == lastType && displayLastTimestamp) {
            // bottom corner bubble
            if (currentType == Message.TYPE_RECEIVED) {
                R.drawable.message_circle_received_group_bottom_background
            } else {
                R.drawable.message_circle_sent_group_bottom_background
            }
        } else {
            R.drawable.message_circle_background
        }
    }

    @DrawableRes
    private fun materialThemeStyleBackground(): Int {
        val displayNextTimestamp = TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp)
        val displayLastTimestamp = TimeUtils.shouldDisplayTimestamp(lastTimestamp, currentTimestamp)

        return if (sameType(currentType, lastType) && sameType(currentType, nextType) && !displayLastTimestamp && !displayNextTimestamp) {
            // both corners
            if (currentType == Message.TYPE_RECEIVED) {
                R.drawable.message_material_theme_received_group_both_background
            } else {
                R.drawable.message_material_theme_sent_group_both_background
            }
        } else if (sameType(currentType, lastType) && !sameType(currentType, nextType) && !displayLastTimestamp || sameType(currentType, nextType) && sameType(currentType, lastType) && displayNextTimestamp && !displayLastTimestamp) {
            // top corner bubble
            if (currentType == Message.TYPE_RECEIVED) {
                R.drawable.message_material_theme_received_group_top_background
            } else {
                R.drawable.message_material_theme_sent_group_top_background
            }
        } else if (sameType(currentType, nextType) && !sameType(currentType, lastType) && !displayNextTimestamp || sameType(currentType, nextType) && sameType(currentType, lastType) && displayLastTimestamp && !displayNextTimestamp) {
            // bottom corner bubble
            if (currentType == Message.TYPE_RECEIVED) {
                R.drawable.message_material_theme_received_group_bottom_background
            } else {
                R.drawable.message_material_theme_sent_group_bottom_background
            }
        } else {
            if (currentType == Message.TYPE_RECEIVED) {
                R.drawable.message_material_theme_received_background
            } else {
                R.drawable.message_material_theme_sent_background
            }
        }
    }

    private fun sameType(one: Int, two: Int): Boolean {
        return when {
            one == two -> true
            one == Message.TYPE_SENDING && (two == Message.TYPE_SENT || two == Message.TYPE_DELIVERED || two == Message.TYPE_ERROR) -> true
            two == Message.TYPE_SENDING && (one == Message.TYPE_SENT || one == Message.TYPE_DELIVERED || one == Message.TYPE_ERROR) -> true
            else -> false
        }
    }
}
