/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger

import android.app.Application
import android.content.Intent
import android.os.Build
import android.support.v7.app.AppCompatDelegate
import xyz.klinker.messenger.api.implementation.firebase.FirebaseApplication
import xyz.klinker.messenger.api.implementation.firebase.FirebaseMessageHandler
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.BaseTheme
import xyz.klinker.messenger.shared.service.CreateNotificationChannelService
import xyz.klinker.messenger.shared.service.FirebaseHandlerService
import xyz.klinker.messenger.shared.service.FirebaseResetService
import xyz.klinker.messenger.shared.service.QuickComposeNotificationService
import xyz.klinker.messenger.shared.util.AndroidVersionUtil
import xyz.klinker.messenger.shared.util.DynamicShortcutUtils
import xyz.klinker.messenger.shared.util.KotlinObjectInitializers
import xyz.klinker.messenger.shared.util.TimeUtils

/**
 * Base application that will serve as any intro for any context in the rest of the app. Main
 * function is to enable night mode so that colors change depending on time of day.
 */
class MessengerApplication : FirebaseApplication() {

    override fun onCreate() {
        super.onCreate()

        KotlinObjectInitializers.initializeObjects(this)

        enableSecurity()

        val theme = Settings.baseTheme
        if (theme === BaseTheme.ALWAYS_LIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else if (theme.isDark || TimeUtils.isNight) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        if (CreateNotificationChannelService.shouldRun(this)) {
            startForegroundService(Intent(this, CreateNotificationChannelService::class.java))
        }

        if (Settings.quickCompose) {
            QuickComposeNotificationService.start(this)
        }
    }

    fun refreshDynamicShortcuts() {
        if ("robolectric" != Build.FINGERPRINT && AndroidVersionUtil.isAndroidN_MR1 && !Settings.firstStart) {
            Thread {
                try {
                    Thread.sleep(10 * TimeUtils.SECOND)
                    val source = DataSource

                    var conversations = source.getPinnedConversationsAsList(this)
                    if (conversations.isEmpty()) {
                        conversations = source.getUnarchivedConversationsAsList(this)
                    }

                    DynamicShortcutUtils(this@MessengerApplication).buildDynamicShortcuts(conversations)
                } catch (e: Exception) {
                }
            }.start()
        }
    }

    override fun getFirebaseMessageHandler(): FirebaseMessageHandler {
        return object : FirebaseMessageHandler {
            override fun handleMessage(application: Application, operation: String, data: String) {
                Thread { FirebaseHandlerService.process(application, operation, data) }.start()
            }

            override fun handleDelete(application: Application) {
                val handleMessage = Intent(application, FirebaseResetService::class.java)
                if (AndroidVersionUtil.isAndroidO) {
                    startForegroundService(handleMessage)
                } else {
                    startService(handleMessage)
                }
            }
        }
    }

    companion object {

        /**
         * Enable night mode and set it to auto. It will switch depending on time of day.
         */
        init {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
        }

        /**
         * By default, java does not allow for strong security schemes due to export laws in other
         * countries. This gets around that. Might not be necessary on Android, but we'll put it here
         * anyways just in case.
         */
        private fun enableSecurity() {
            try {
                val field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted")
                field.isAccessible = true
                field.set(null, java.lang.Boolean.FALSE)
            } catch (e: Exception) {

            }

        }
    }
}
