package xyz.klinker.messenger.shared.util;

import android.os.Build;

public class AndroidVersionUtil {
    public static boolean isAndroidO() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || Build.VERSION.CODENAME.equals("O");
    }
}
