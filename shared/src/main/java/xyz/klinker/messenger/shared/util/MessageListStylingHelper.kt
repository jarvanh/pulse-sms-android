package xyz.klinker.messenger.shared.util

import android.content.Context
import android.database.Cursor
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.data.pojo.BubbleTheme

class MessageListStylingHelper(context: Context?) {

    private val eightDp: Int = DensityUtil.toDp(context, 8)

    private var columnType = -1
    private var columnTimestamp = -1
    private var columnContact = -1

    private var currentType: Int = 0
    private var currentTimestamp: Long = 0
    private var currentFrom: String? = null

    private var lastType: Int = 0
    private var lastTimestamp: Long = 0
    private var lastFrom: String? = null

    private var nextType: Int = 0
    private var nextTimestamp: Long = 0
    private var nextFrom: String? = null

    var hideContact = false


    fun calculateAdjacentItems(cursor: Cursor, currentPosition: Int): MessageListStylingHelper {
        if (columnTimestamp == -1) {
            columnTimestamp = cursor.getColumnIndex(Message.COLUMN_TIMESTAMP)
        }

        if (columnType == -1) {
            columnType = cursor.getColumnIndex(Message.COLUMN_TYPE)
        }

        if (columnContact == -1) {
            columnContact = cursor.getColumnIndex(Message.COLUMN_FROM)
        }

        cursor.moveToPosition(currentPosition)
        currentType = cursor.getInt(columnType)
        currentTimestamp = cursor.getLong(columnTimestamp)
        currentFrom = cursor.getString(columnContact)

        if (currentPosition > 0) {
            cursor.moveToPosition(currentPosition - 1)
            lastType = cursor.getInt(columnType)
            lastTimestamp = cursor.getLong(columnTimestamp)
            lastFrom = cursor.getString(columnContact)
        } else {
            lastType = -1
            lastTimestamp = -1
            lastFrom = null
        }

        if (currentPosition != cursor.count - 1) {
            cursor.moveToPosition(currentPosition + 1)
            nextType = cursor.getInt(columnType)
            nextTimestamp = cursor.getLong(columnTimestamp)
            nextFrom = cursor.getString(columnContact)
        } else {
            nextType = -1
            nextTimestamp = TimeUtils.now
            nextFrom = null
        }

        return this
    }

    fun setMargins(itemView: View): MessageListStylingHelper {
        if (itemView.layoutParams == null) {
            return this
        }

        if (!sameType(currentType, lastType) || !sameContact(currentFrom, lastFrom)) {
            (itemView.layoutParams as RecyclerView.LayoutParams).topMargin = eightDp
        } else {
            (itemView.layoutParams as RecyclerView.LayoutParams).topMargin = 0
        }

        if (!sameType(currentType, nextType) || !sameContact(currentFrom, nextFrom) || TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp)) {
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
                BubbleTheme.SQUARE -> fourTypeBackground(if (currentType == Message.TYPE_RECEIVED) squareReceived else squareSent)
                BubbleTheme.ROUNDED -> fourTypeBackground(if (currentType == Message.TYPE_RECEIVED) roundReceived else roundSent)
                BubbleTheme.CIRCLE -> fourTypeBackground(if (currentType == Message.TYPE_RECEIVED) circleReceived else circleSent)
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
    private fun fourTypeBackground(set: DrawableHolder): Int {
        val displayNextTimestamp = TimeUtils.shouldDisplayTimestamp(currentTimestamp, nextTimestamp)
        val displayLastTimestamp = TimeUtils.shouldDisplayTimestamp(lastTimestamp, currentTimestamp)

        return if (same(currentFrom, lastFrom, currentType, lastType) && same(currentFrom, nextFrom, currentType, nextType) && !displayLastTimestamp && !displayNextTimestamp) {
            // both corners
            hideContact = true
            set.groupBoth
        } else if ((same(currentFrom, lastFrom, currentType, lastType) && !same(currentFrom, nextFrom, currentType, nextType) && !displayLastTimestamp) ||
                (same(currentFrom, nextFrom, currentType, nextType) && same(currentFrom, lastFrom, currentType, lastType) && displayNextTimestamp && !displayLastTimestamp)) {
            // top corner bubble
            hideContact = false
            set.groupTop
        } else if ((same(currentFrom, nextFrom, currentType, nextType) && !same(currentFrom, lastFrom, currentType, lastType) && !displayNextTimestamp) ||
                (same(currentFrom, nextFrom, currentType, nextType) && same(currentFrom, lastFrom, currentType, lastType) && displayLastTimestamp && !displayNextTimestamp)) {
            // bottom corner bubble
            hideContact = true
            set.groupBottom
        } else {
            // normal bubble
            hideContact = false
            set.noGrouped
        }
    }

    private fun same(contactOne: String?, contactTwo: String?, typeOne: Int, typeTwo: Int) = sameContact(contactOne, contactTwo) && sameType(typeOne, typeTwo)
    private fun sameContact(one: String?, two: String?) = one == two
    private fun sameType(one: Int, two: Int): Boolean {
        return when {
            one == two -> true
            one == Message.TYPE_SENDING && (two == Message.TYPE_SENT || two == Message.TYPE_DELIVERED || two == Message.TYPE_ERROR) -> true
            two == Message.TYPE_SENDING && (one == Message.TYPE_SENT || one == Message.TYPE_DELIVERED || one == Message.TYPE_ERROR) -> true
            else -> false
        }
    }


    private class DrawableHolder(val groupBoth: Int, val groupTop: Int, val groupBottom: Int, val noGrouped: Int)

    companion object {
        private val roundReceived = DrawableHolder(
                R.drawable.message_round_received_group_both_background,
                R.drawable.message_round_received_group_top_background,
                R.drawable.message_round_received_group_bottom_background,
                R.drawable.message_round_received_background
        )

        private val roundSent = DrawableHolder(
                R.drawable.message_round_sent_group_both_background,
                R.drawable.message_round_sent_group_top_background,
                R.drawable.message_round_sent_group_bottom_background,
                R.drawable.message_round_sent_background
        )

        private val circleReceived = DrawableHolder(
                R.drawable.message_circle_received_group_both_background,
                R.drawable.message_circle_received_group_top_background,
                R.drawable.message_circle_received_group_bottom_background,
                R.drawable.message_circle_background
        )

        private val circleSent = DrawableHolder(
                R.drawable.message_circle_sent_group_both_background,
                R.drawable.message_circle_sent_group_top_background,
                R.drawable.message_circle_sent_group_bottom_background,
                R.drawable.message_circle_background
        )

        private val squareReceived = DrawableHolder(
                R.drawable.message_square_received_group_background,
                R.drawable.message_square_received_group_background,
                R.drawable.message_square_received_background,
                R.drawable.message_square_received_background
        )

        private val squareSent = DrawableHolder(
                R.drawable.message_square_sent_group_background,
                R.drawable.message_square_sent_group_background,
                R.drawable.message_square_sent_background,
                R.drawable.message_square_sent_background
        )
    }
}


