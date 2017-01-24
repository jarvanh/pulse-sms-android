package xyz.klinker.messenger.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class DensityUtil {

    public static int toPx(Context context, int dp) {
        return convert(context, dp, TypedValue.COMPLEX_UNIT_PX);
    }

    public static int toDp(Context context, int px) {
        return convert(context, px, TypedValue.COMPLEX_UNIT_DIP);
    }

    private static int convert(Context context, int amount, int conversionUnit) {
        if (amount < 0) {
            throw new IllegalArgumentException("px should not be less than zero");
        }

        Resources r = context.getResources();
        return (int) TypedValue.applyDimension(conversionUnit, amount, r.getDisplayMetrics());
    }

    public static int spToPx(Context context, int sp) {
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (sp * scaledDensity);
    }
}