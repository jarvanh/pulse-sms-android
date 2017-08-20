package xyz.klinker.messenger.shared.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;

public class ContactImageCreator {

    public static Bitmap getLetterPicture(Context context, Conversation conversation) {
        int backgroundColor = conversation.colors.color;
        if (Settings.get(context).useGlobalThemeColor) {
            backgroundColor = Settings.get(context).mainColorSet.color;
        }

        if (conversation.title.length() == 0 || conversation.title.contains(", ")) {
            Bitmap color = Bitmap.createBitmap(DensityUtil.toDp(context, 48), DensityUtil.toDp(context, 48), Bitmap.Config.ARGB_8888);
            color.eraseColor(backgroundColor);
            return ImageUtils.clipToCircle(color);
        }

        int size = DensityUtil.toDp(context, 72);
        Bitmap image;

        try {
            image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError e) {
            size /= 2;
            image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(image);
        canvas.drawColor(backgroundColor);

        Paint textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(ColorUtils.isColorDark(backgroundColor) ? context.getResources().getColor(android.R.color.white) :
                context.getResources().getColor(R.color.lightToolbarTextColor));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize((int) (size / 1.5));

        try {
            canvas.drawText(conversation.title.substring(0, 1).toUpperCase(), canvas.getWidth() / 2,
                    (int) ((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)),
                    textPaint);

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return ImageUtils.clipToCircle(image);
    }
}
