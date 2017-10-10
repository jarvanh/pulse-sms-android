package xyz.klinker.messenger.shared.util;

import android.content.Context;
import android.content.res.Configuration;

public class WearableCheck {

    public static boolean isAndroidWear(Context context) {
        Configuration config = context.getResources().getConfiguration();
        return (config.uiMode & Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_WATCH;
    }
}
