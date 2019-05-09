package xyz.klinker.messenger.shared.util

import android.os.Build

object AndroidVersionUtil {
    val isAndroidQ: Boolean
        get() = Build.VERSION.SDK_INT > Build.VERSION_CODES.P || Build.VERSION.CODENAME == "Q"

    val isAndroidP: Boolean
        get() = Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1 || Build.VERSION.CODENAME == "P"

    val isAndroidO_MR1: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

    val isAndroidO: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || Build.VERSION.CODENAME == "O"

    val isAndroidN_MR1: Boolean
        get() =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

    val isAndroidN: Boolean
        get() =  Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
}
