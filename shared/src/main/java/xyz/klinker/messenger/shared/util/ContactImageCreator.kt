package xyz.klinker.messenger.shared.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.model.Conversation

object ContactImageCreator {

    fun getLetterPicture(context: Context, conversation: Conversation): Bitmap {
        var backgroundColor = conversation.colors.color
        if (Settings.useGlobalThemeColor) {
            backgroundColor = Settings.mainColorSet.color
        }

        if (conversation.title!!.isEmpty() || conversation.title!!.contains(", ")) {
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

        val textPaint = Paint()
        textPaint.style = Paint.Style.FILL
        textPaint.color = if (backgroundColor.isDarkColor())
            context.resources.getColor(android.R.color.white)
        else
            context.resources.getColor(R.color.lightToolbarTextColor)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isAntiAlias = true
        textPaint.textSize = (size / 1.5).toInt().toFloat()

        try {
            canvas.drawText(conversation.title!!.substring(0, 1).toUpperCase(), (canvas.width / 2).toFloat(),
                    (canvas.height / 2 - (textPaint.descent() + textPaint.ascent()) / 2).toInt().toFloat(),
                    textPaint)

        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return ImageUtils.clipToCircle(image)
    }
}
