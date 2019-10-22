package xyz.klinker.messenger.shared.util

import android.annotation.SuppressLint
import android.content.Context
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings

object FirstRunInitializer {

    @SuppressLint("ApplySharedPref")
    fun applyDefaultSettings(context: Context) {
        if (Settings.firstStart) {
            Settings.setValue(context, context.getString(R.string.pref_conversation_categories), false)
            Settings.setValue(context, context.getString(R.string.pref_apply_primary_color_toolbar), false)
            Settings.setValue(context, context.getString(R.string.pref_base_theme), if (AndroidVersionUtil.isAndroidQ) "day_night" else "dark")
        }
    }
}