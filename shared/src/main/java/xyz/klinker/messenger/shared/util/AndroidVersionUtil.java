package xyz.klinker.messenger.shared.util;

import android.os.Build;

public class AndroidVersionUtil {
    public static boolean isAndroidO() {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1 || Build.VERSION.CODENAME.equals("O");
    }
}
