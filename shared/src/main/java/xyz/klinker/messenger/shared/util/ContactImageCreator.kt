@file:Suppress("DEPRECATION")

package xyz.klinker.messenger.shared.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.data.pojo.NotificationConversation

object ContactImageCreator {

    fun getLetterPicture(context: Context?, conversation: Conversation?): Bitmap? {
        if (context == null || conversation == null) {
            return null
        }

        return getLetterPicture(context, conversation.title!!, conversation.colors.color)
    }

    fun getLetterPicture(context: Context, notificationConversation: NotificationConversation): Bitmap? {
        return getLetterPicture(context, notificationConversation.title!!, notificationConversation.color)
    }

    private fun getLetterPicture(context: Context, title: String, conversationColor: Int): Bitmap? {
        var backgroundColor = conversationColor
        if (Settings.useGlobalThemeColor) {
            backgroundColor = Settings.mainColorSet.color
        }

        if (title.isEmpty()) {
            val color = Bitmap.createBitmap(DensityUtil.toDp(context, 48), DensityUtil.toDp(context, 48), Bitmap.Config.ARGB_8888)
            color.eraseColor(backgroundColor)
            return ImageUtils.clipToCircle(color)
        }

        var size = DensityUtil.toDp(context, 72)
        var image: Bitmap

        try {
            image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            size /= 2
            image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        }

        val canvas = Canvas(image)
        canvas.drawColor(backgroundColor)

        if (title.contains(",")) {
            try {
                val edge = size / 4
                val drawable = context.resources.getDrawable(R.drawable.ic_group)
                drawable.setBounds(edge, edge, size - edge, size - edge)
                drawable.draw(canvas)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        } else {
            val textPaint = Paint()
            textPaint.style = Paint.Style.FILL
            textPaint.color = if (backgroundColor.isDarkColor())
                context.resources.getColor(android.R.color.white)
            else
                context.resources.getColor(R.color.lightToolbarTextColor)
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.isAntiAlias = true
            textPaint.textSize = (size / 2).toInt().toFloat()

            try {
                canvas.drawText(title.substring(0, 1).toUpperCase(), (canvas.width / 2).toFloat(),
                        (canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2).toInt().toFloat(),
                        textPaint)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        return ImageUtils.clipToCircle(image)
    }
}
