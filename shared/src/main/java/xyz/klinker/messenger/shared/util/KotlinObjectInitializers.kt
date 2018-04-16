package xyz.klinker.messenger.shared.util

import android.content.Context
import com.github.ajalt.reprint.core.Reprint
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.data.Settings

object KotlinObjectInitializers {

    fun initializeObjects(context: Context) {
        try {
            ApiUtils.environment = context.getString(R.string.environment)
        } catch (e: Exception) {
            ApiUtils.environment = "release"
        }

        Account.init(context)
        FeatureFlags.init(context)
        Settings.init(context)
        MmsSettings.init(context)
        DualSimUtils.init(context)
        EmojiInitializer.initializeEmojiCompat(context)

        try {
            Reprint.initialize(context)
        } catch (t: Throwable) {
        }
    }
}