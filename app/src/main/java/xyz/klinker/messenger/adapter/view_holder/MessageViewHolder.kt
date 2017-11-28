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

package xyz.klinker.messenger.adapter.view_holder

import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AlertDialog
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bignerdranch.android.multiselector.MultiSelector
import com.bignerdranch.android.multiselector.SwappingHolder
import xyz.klinker.android.article.ArticleIntent
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.ImageViewerActivity
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.shared.data.*
import xyz.klinker.messenger.shared.data.model.Message
import xyz.klinker.messenger.shared.receiver.MessageListUpdatedReceiver
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.DensityUtil
import xyz.klinker.messenger.shared.util.ImageUtils
import xyz.klinker.messenger.shared.util.MediaSaver
import xyz.klinker.messenger.shared.util.listener.ForcedRippleTouchListener
import xyz.klinker.messenger.shared.util.listener.MessageDeletedListener
import xyz.klinker.messenger.shared.util.media.parsers.ArticleParser

@Suppress("DEPRECATION")
/**
 * View holder for working with a message.
 */
class MessageViewHolder(private val fragment: MessageListFragment?, itemView: View, color: Int, private val type: Int,
                        private val messageDeletedListener: MessageDeletedListener?)
    : SwappingHolder(itemView, fragment?.multiSelect ?: MultiSelector()) {

    private val activity: FragmentActivity? by lazy { fragment?.activity }
    private val timestampHeight: Int by lazy { DensityUtil.spToPx(itemView.context, Settings.mediumFont + 2)}

    val title: TextView by lazy { itemView.findViewById<View>(R.id.title) as TextView }
    val timestamp: TextView by lazy { itemView.findViewById<View>(R.id.timestamp) as TextView }
    val message: TextView? by lazy { itemView.findViewById<View>(R.id.message) as TextView? }
    val contact: TextView? by lazy { itemView.findViewById<View>(R.id.contact) as TextView? }
    val image: ImageView? by lazy { itemView.findViewById<View>(R.id.image) as ImageView? }
    val clippedImage: ImageView? by lazy { itemView.findViewById<View>(R.id.clipped_image) as ImageView? }
    val messageHolder: View? by lazy { itemView.findViewById<View>(R.id.message_holder) }

    var messageId: Long = 0
    var data: String? = null
    var mimeType: String? = null

    var color = Integer.MIN_VALUE
    var textColor = Integer.MIN_VALUE

    private var primaryColor = Integer.MIN_VALUE
    private var accentColor = Integer.MIN_VALUE

    init {
        image?.clipToOutline = true

        if (type != Message.TYPE_MEDIA && type != Message.TYPE_IMAGE_SENDING) {
            timestamp.textSize = Settings.smallFont.toFloat()
            timestamp.height = DensityUtil.spToPx(itemView.context, Settings.mediumFont)
            contact?.textSize = Settings.smallFont.toFloat()
            contact?.height = DensityUtil.spToPx(itemView.context, Settings.mediumFont)
        }

        val useGlobalThemeColor = Settings.useGlobalThemeColor
        if (color != Integer.MIN_VALUE && messageHolder != null || useGlobalThemeColor && type == Message.TYPE_RECEIVED) {
            if (useGlobalThemeColor) {
                this.color = Settings.mainColorSet.color
            } else {
                this.color = color
            }

            textColor = if (!ColorUtils.isColorDark(this.color)) {
                itemView.context.resources.getColor(R.color.darkText)
            } else {
                itemView.context.resources.getColor(R.color.lightText)
            }

            message?.setTextColor(textColor)
            messageHolder?.backgroundTintList = ColorStateList.valueOf(this.color)
        }

        image?.setOnClickListener {
            if (fragment?.multiSelect != null && fragment.multiSelect.isSelectable) {
                messageHolder?.performClick()
                return@setOnClickListener
            }

            if (mimeType != null && MimeType.isVcard(mimeType!!)) {
                var uri = Uri.parse(message?.text.toString())
                if (message?.text.toString().contains("file://")) {
                    uri = ImageUtils.createContentUri(itemView.context, uri)
                }

                val intent = Intent(Intent.ACTION_VIEW)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.setDataAndType(uri, MimeType.TEXT_VCARD)

                try {
                    itemView.context.startActivity(intent)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            } else if (mimeType == MimeType.MEDIA_YOUTUBE_V2) {
                val preview = YouTubePreview.build(data!!)
                if (preview != null) {
                    try {
                        itemView.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(preview.url)))
                    } catch (e: ActivityNotFoundException) {
                        // Android TV
                    }
                }
            } else if (mimeType == MimeType.MEDIA_ARTICLE) {
                startArticle()
            } else {
                val intent = Intent(itemView.context, ImageViewerActivity::class.java)
                intent.putExtra(ImageViewerActivity.EXTRA_CONVERSATION_ID, fragment?.conversationId)
                intent.putExtra(ImageViewerActivity.EXTRA_MESSAGE_ID, messageId)

                itemView.context.startActivity(intent)
            }
        }

        if (fragment != null) {
            image?.setOnLongClickListener(getMessageLongClickListener())
            image?.isHapticFeedbackEnabled = false
            message?.setOnLongClickListener(getMessageLongClickListener())
            message?.setOnClickListener(getMessageClickListener())
        }

        message?.setOnTouchListener(ForcedRippleTouchListener(message!!))
        message?.isHapticFeedbackEnabled = false
        messageHolder?.setOnClickListener(getMessageClickListener())
    }

    private fun showMessageDetails() {
        val source = DataSource
        AlertDialog.Builder(itemView.context)
                .setMessage(source.getMessageDetails(itemView.context, messageId))
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    private fun deleteMessage() {
        AlertDialog.Builder(itemView.context)
                .setTitle(R.string.delete_message)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val m = DataSource.getMessage(itemView.context, messageId)

                    if (m != null) {
                        val conversationId = m.conversationId
                        DataSource.deleteMessage(itemView.context, messageId)
                        MessageListUpdatedReceiver.sendBroadcast(itemView.context, conversationId)
                    }

                    if (messageDeletedListener != null && m != null) {
                        messageDeletedListener.onMessageDeleted(itemView.context, m.conversationId,
                                adapterPosition)
                    }
                }
                .setNegativeButton(R.string.no, null).show()
    }

    private fun copyMessageText() {
        val clipboard = itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("messenger", message?.text.toString())
        clipboard.primaryClip = clip
        Toast.makeText(itemView.context, R.string.message_copied_to_clipboard,
                Toast.LENGTH_SHORT).show()
    }

    private fun resendMessage() {
        fragment?.resendMessage(messageId, message?.text.toString())
    }

    private fun shareImage(messageId: Long) {
        val message = getMessage(messageId)

        val contentUri = ImageUtils.createContentUri(itemView.context, Uri.parse(message!!.data))

        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        shareIntent.type = message.mimeType
        itemView.context.startActivity(Intent.createChooser(shareIntent,
                itemView.context.resources.getText(R.string.share_content)))
    }

    private fun shareText(messageId: Long) {
        val message = getMessage(messageId)

        if (message != null) {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_TEXT, message.data)
            shareIntent.type = message.mimeType
            itemView.context.startActivity(Intent.createChooser(shareIntent,
                    itemView.context.resources.getText(R.string.share_content)))
        }
    }

    private fun getMessage(messageId: Long): Message? {
        return DataSource.getMessage(itemView.context, messageId)
    }

    fun setColors(color: Int, accentColor: Int) {
        this.primaryColor = color
        this.accentColor = accentColor
    }

    private fun startArticle() {
        val intent = ArticleIntent.Builder(itemView.context, ArticleParser.ARTICLE_API_KEY)
                .setToolbarColor(primaryColor)
                .setAccentColor(accentColor)
                .setTheme(if (Settings.isCurrentlyDarkTheme)
                    ArticleIntent.THEME_DARK
                else
                    ArticleIntent.THEME_LIGHT)
                .setTextSize(Settings.mediumFont + 1)
                .build()

        val preview = ArticlePreview.build(data!!)
        if (preview != null) {
            if (Settings.internalBrowser) {
                intent.launchUrl(itemView.context, Uri.parse(preview.webUrl))
            } else {
                val url = Intent(Intent.ACTION_VIEW)
                url.data = Uri.parse(preview.webUrl)

                try {
                    itemView.context.startActivity(url)
                } catch (e: Exception) {

                }
            }
        }
    }

    private fun getMessageClickListener() = View.OnClickListener {
        if (type == Message.TYPE_INFO || fragment?.multiSelect == null ||
                fragment.multiSelect.tapSelection(this@MessageViewHolder)) {
            return@OnClickListener
        } else if (mimeType == MimeType.MEDIA_ARTICLE) {
            startArticle()
            return@OnClickListener
        }

        val animator: ValueAnimator
        if (timestamp.height > 0) {
            animator = ValueAnimator.ofInt(timestampHeight, 0)
            animator.interpolator = AccelerateInterpolator()
        } else {
            animator = ValueAnimator.ofInt(0, timestampHeight)
            animator.interpolator = DecelerateInterpolator()
        }

        val params = timestamp.layoutParams
        animator.addUpdateListener { animation ->
            params.height = animation.animatedValue as Int
            timestamp.requestLayout()
        }
        animator.duration = 100
        animator.start()
    }

    private fun getMessageLongClickListener() = View.OnLongClickListener { view ->
        if (fragment == null || fragment.isRecyclerScrolling) {
            return@OnLongClickListener true
        }

        if (MimeType.isExpandedMedia(mimeType) || message != null && message!!.visibility == View.VISIBLE && type != Message.TYPE_ERROR &&
                type != Message.TYPE_INFO) {

            if (!fragment.multiSelect.isSelectable) {
                // start the multi-select
                fragment.multiSelect.startActionMode()
                fragment.multiSelect.isSelectable = true
                fragment.multiSelect.setSelected(this@MessageViewHolder, true)
            }

            return@OnLongClickListener true
        }

        val items: Array<String?>
        if (message != null && message!!.visibility == View.VISIBLE) {
            if (type == Message.TYPE_ERROR) {
                items = arrayOfNulls(5)
                items[4] = view.context.getString(R.string.resend)
            } else {
                items = arrayOfNulls(4)
            }

            items[0] = view.context.getString(R.string.view_details)
            items[1] = view.context.getString(R.string.delete)
            items[2] = view.context.getString(R.string.copy_message)
            items[3] = view.context.getString(R.string.share)
        } else {
            if (image!!.visibility == View.VISIBLE) {
                items = arrayOfNulls(4)
                items[3] = view.context.getString(R.string.save)
                items[2] = view.context.getString(R.string.share)
            } else {
                items = arrayOfNulls(2)
            }

            items[0] = view.context.getString(R.string.view_details)
            items[1] = view.context.getString(R.string.delete)
        }

        if (!fragment.isDragging) {
            val dialog = AlertDialog.Builder(view.context)
                    .setItems(items) { _, which ->
                        when {
                            which == 0 -> showMessageDetails()
                            which == 1 -> deleteMessage()
                            which == 2 && image?.visibility == View.VISIBLE -> shareImage(messageId)
                            which == 2 -> copyMessageText()
                            which == 3 && image?.visibility == View.VISIBLE -> MediaSaver(activity).saveMedia(messageId)
                            which == 3 -> shareText(messageId)
                            which == 4 -> resendMessage()
                        }
                    }.show()

            fragment.setDetailsChoiceDialog(dialog)
        }

        // need to manually force the haptic feedback, since the feedback was actually
        // disabled on the long clicked views
        view.isHapticFeedbackEnabled = true
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        view.isHapticFeedbackEnabled = false

        false
    }
}
