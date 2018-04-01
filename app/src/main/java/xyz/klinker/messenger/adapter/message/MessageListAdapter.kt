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

package xyz.klinker.messenger.adapter.message

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.StaleDataException
import android.os.Build
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.turingtechnologies.materialscrollbar.IDateableAdapter
import xyz.klinker.messenger.R
import xyz.klinker.messenger.adapter.view_holder.MessageViewHolder
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Contact
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.util.DensityUtil
import xyz.klinker.messenger.shared.util.MessageListStylingHelper
import xyz.klinker.messenger.shared.util.TimeUtils
import xyz.klinker.messenger.shared.util.listener.MessageDeletedListener
import java.util.*

/**
 * Adapter for displaying messages in a conversation.
 */
class MessageListAdapter(messages: Cursor, private val receivedColor: Int, private val accentColor: Int, private val isGroup: Boolean,
                         private val fragment: MessageListFragment)
    : RecyclerView.Adapter<MessageViewHolder>(), MessageDeletedListener, IDateableAdapter {

    private val activity: FragmentActivity? by lazy { fragment.activity }

    private var ignoreSendingStatus: Boolean = false
    private var timestampHeight = DensityUtil.spToPx(activity, Settings.mediumFont + 2)

    private val dataProvider: MessageListDataProvider = MessageListDataProvider(this, fragment, messages)
    private val itemBinder: MessageItemBinder = MessageItemBinder(this)
    private val colorHelper: MessageColorHelper = MessageColorHelper()
    private val stylingHelper: MessageListStylingHelper = MessageListStylingHelper(activity)
    private val linkApplier: MessageLinkApplier = MessageLinkApplier(fragment, accentColor, receivedColor)
    private val emojiEnlarger: MessageEmojiEnlarger = MessageEmojiEnlarger()

    private val imageHeight: Int = DensityUtil.toPx(activity, 350)
    private val imageWidth: Int = DensityUtil.toPx(activity, 350)

    var snackbar: Snackbar? = null

    val messages: Cursor
        get() = dataProvider.messages

    init {
        ignoreSendingStatus = Account.exists() && !Account.primary
        if (Build.FINGERPRINT == "robolectric" || FeatureFlags.REENABLE_SENDING_STATUS_ON_NON_PRIMARY) {
            ignoreSendingStatus = false
        }

        fragment.multiSelect.setAdapter(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId: Int
        val color: Int

        val rounder = Settings.rounderBubbles
        if (viewType == Message.TYPE_RECEIVED) {
            layoutId = if (rounder) R.layout.message_received_round else R.layout.message_received
            color = receivedColor
        } else {
            color = Integer.MIN_VALUE
            layoutId = when (viewType) {
                Message.TYPE_SENDING -> if (rounder) R.layout.message_sending_round else R.layout.message_sending
                Message.TYPE_ERROR -> if (rounder) R.layout.message_error_round else R.layout.message_error
                Message.TYPE_DELIVERED -> if (rounder) R.layout.message_delivered_round else R.layout.message_delivered
                Message.TYPE_IMAGE_SENDING -> if (rounder) R.layout.message_image_sending_round else R.layout.message_image_sending
                Message.TYPE_IMAGE_SENT -> if (rounder) R.layout.message_image_sent_round else R.layout.message_image_sent
                Message.TYPE_IMAGE_RECEIVED -> if (rounder) R.layout.message_image_received_round else R.layout.message_image_received
                Message.TYPE_INFO -> R.layout.message_info
                Message.TYPE_MEDIA -> R.layout.message_media
                else -> if (rounder) R.layout.message_sent_round else R.layout.message_sent
            }
        }

        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        val holder = MessageViewHolder(fragment, view, if (isGroup) Integer.MIN_VALUE else color, viewType, this)
        holder.setColors(receivedColor, accentColor)

        return holder
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        try {
            dataProvider.messages.moveToPosition(position)
        } catch (e: IllegalStateException) {
            fragment.onBackPressed()
            return
        }

        val message = Message()
        message.fillFromCursor(dataProvider.messages)

        holder.messageId = message.id
        holder.mimeType = message.mimeType
        holder.data = message.data

        val backgroundColor = colorHelper.getColor(holder, message)

        if (message.mimeType == MimeType.TEXT_PLAIN) {
            holder.message?.text = message.data
            emojiEnlarger.enlarge(holder, message)
            linkApplier.apply(holder, message, backgroundColor)

            itemBinder.setGone(holder.image)
            itemBinder.setVisible(holder.message)
        } else {
            if (!MimeType.isExpandedMedia(message.mimeType) || message.mimeType == MimeType.MEDIA_MAP) {
                holder.image?.setImageDrawable(null)
                holder.image?.minimumWidth = imageWidth
                holder.image?.minimumHeight = imageHeight
                itemBinder.setGone(holder.message)
            }

            when {
                MimeType.isStaticImage(message.mimeType) -> itemBinder.staticImage(holder)
                MimeType.isVideo(message.mimeType!!) -> itemBinder.video(holder, position)
                MimeType.isAudio(message.mimeType!!) -> itemBinder.audio(holder, position)
                MimeType.isVcard(message.mimeType!!) -> itemBinder.vCard(holder, position)
                message.mimeType == MimeType.IMAGE_GIF -> itemBinder.animatedGif(holder)
                message.mimeType == MimeType.MEDIA_YOUTUBE_V2 -> itemBinder.youTube(holder)
                message.mimeType == MimeType.MEDIA_TWITTER -> itemBinder.twitter(holder)
                message.mimeType == MimeType.MEDIA_MAP -> itemBinder.map(holder)
                message.mimeType == MimeType.MEDIA_ARTICLE -> itemBinder.article(holder)
                else -> Log.v("MessageListAdapter", "unused mime type: " + message.mimeType!!)
            }

            itemBinder.setVisible(holder.image)
        }

        if (message.simPhoneNumber != null) {
            holder.timestamp.text = TimeUtils.formatTimestamp(holder.timestamp.context,
                    message.timestamp) + " (SIM " + message.simPhoneNumber + ")"
        } else {
            holder.timestamp.text = TimeUtils.formatTimestamp(holder.itemView.context,
                    message.timestamp)
        }

        if (!isGroup) {
            stylingHelper.calculateAdjacentItems(dataProvider.messages, position)
                    .setMargins(holder.itemView)
                    .setBackground(holder.messageHolder, message.mimeType!!)
                    .applyTimestampHeight(holder.timestamp, timestampHeight)
        } else {
            stylingHelper.calculateAdjacentItems(dataProvider.messages, position)
                    .applyTimestampHeight(holder.timestamp, timestampHeight)

            if (holder.contact != null && !MimeType.isExpandedMedia(message.mimeType!!)) {
                holder.contact!!.layoutParams.height = timestampHeight
            }

            val label = if (holder.timestamp.layoutParams.height > 0)
                R.string.message_from_bullet else R.string.message_from

            if (!MimeType.isExpandedMedia(message.mimeType)) {
                holder.contact?.text = holder.itemView.resources.getString(label, message.from)
                itemBinder.setVisible(holder.contact)
            }
        }
    }

    override fun getItemCount() = try { dataProvider.messages.count } catch (e: Exception) { 0 }

    override fun getItemViewType(position: Int): Int {
        try {
            dataProvider.messages.moveToPosition(position)
            var type = dataProvider.messages.getInt(dataProvider.messages.getColumnIndex(Message.COLUMN_TYPE))
            val mimeType = dataProvider.messages.getString(dataProvider.messages.getColumnIndex(Message.COLUMN_MIME_TYPE))

            if (ignoreSendingStatus && type == Message.TYPE_SENDING) {
                type = if (mimeType != null && (mimeType.contains("image") || mimeType.contains("video") || mimeType == MimeType.MEDIA_MAP))
                    Message.TYPE_IMAGE_SENT else Message.TYPE_SENT
            } else if (mimeType != null && (mimeType.contains("image") || mimeType.contains("video") || mimeType == MimeType.MEDIA_MAP)) {
                type = when (type) {
                    Message.TYPE_RECEIVED -> Message.TYPE_IMAGE_RECEIVED
                    Message.TYPE_SENDING -> Message.TYPE_IMAGE_SENDING
                    else -> Message.TYPE_IMAGE_SENT
                }
            }

            return type
        } catch (e: Exception) {
            return -1
        }

    }

    override fun getDateForElement(position: Int): Date {
        if (position < 0) {
            return Date()
        }

        try {
            dataProvider.messages.moveToPosition(position)
        } catch (e: Exception) {
            fragment.onBackPressed()
            return Date()
        }
        
        val millis = dataProvider.messages.getLong(dataProvider.messages.getColumnIndex(Message.COLUMN_TIMESTAMP))
        return Date(millis)
    }

    override fun getItemId(position: Int) = try {
            dataProvider.messages.moveToPosition(position)
            dataProvider.messages.getLong(dataProvider.messages.getColumnIndex(Message.COLUMN_ID))
        } catch (e: Exception) {
            -1L
        }

    fun addMessage(recycler: RecyclerView, newMessages: Cursor) {
        dataProvider.addMessage(recycler, newMessages)
    }

    override fun onMessageDeleted(context: Context, conversationId: Long, position: Int) {
        dataProvider.onMessageDeleted(context, conversationId, position)
    }

    fun setFromColorMapper(colorMapper: Map<String, Contact>, colorMapperByName: Map<String, Contact>) {
        colorHelper.setMappers(colorMapper, colorMapperByName)
    }
}
