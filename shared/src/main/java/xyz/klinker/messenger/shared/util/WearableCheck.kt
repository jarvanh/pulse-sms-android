package xyz.klinker.messenger.shared.util

import android.content.Context
import android.content.res.Configuration

object WearableCheck {
    fun isAndroidWear(context: Context): Boolean {
        val config = context.resources.configuration
        return config.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_WATCH
    }
}
