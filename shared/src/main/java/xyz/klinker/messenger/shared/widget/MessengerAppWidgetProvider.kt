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

package xyz.klinker.messenger.shared.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.BaseTheme
import xyz.klinker.messenger.shared.util.ActivityUtils
import xyz.klinker.messenger.shared.util.ImageUtils

class MessengerAppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        superOnReceive(context, intent)

        val mgr = getAppWidgetManager(context)

        val action = intent.action
        if (action != null && action == OPEN_ACTION) {
            val itemId = intent.getLongExtra(EXTRA_ITEM_ID, 0)
            val openConversation = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)
            openConversation.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, itemId)
            openConversation.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(openConversation)
            return
        }

        val thisAppWidget = ComponentName(context.packageName, MessengerAppWidgetProvider::class.java.name)
        val appWidgetIds = mgr.getAppWidgetIds(thisAppWidget)

        try {
            mgr.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
        } catch (e: NullPointerException) {
            Log.e(TAG, "failed to notify of widget changed", e)
        }

    }

    fun getAppWidgetManager(context: Context): AppWidgetManager {
        return AppWidgetManager.getInstance(context)
    }

    fun superOnReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (i in appWidgetIds.indices) {
            val intent = Intent(context, MessengerWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i])
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))

            val compose = ActivityUtils.buildForComponent(ActivityUtils.COMPOSE_ACTIVITY)
            val pendingCompose = PendingIntent.getActivity(context, 0, compose, 0)

            val open = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY)
            open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingOpen = PendingIntent.getActivity(context, 0, open, 0)

            val rv = RemoteViews(context.packageName, R.layout.appwidget)
            rv.setRemoteAdapter(R.id.widget_list, intent)
            rv.setOnClickPendingIntent(R.id.compose, pendingCompose)
            rv.setOnClickPendingIntent(R.id.title, pendingOpen)

            rv.setEmptyView(R.id.widget_list, R.id.widget_empty)

            when(Settings.baseTheme) {
                BaseTheme.ALWAYS_DARK -> rv.setInt(R.id.widget_list, "setBackgroundColor", Color.parseColor("#202B30"))
                BaseTheme.BLACK -> rv.setInt(R.id.widget_list, "setBackgroundColor", Color.BLACK)
                else -> rv.setInt(R.id.widget_list, "setBackgroundColor", context.resources.getColor(R.color.background))
            }

            baseWidgetTheme = Settings.baseTheme

            val color = ImageUtils.createColoredBitmap(Settings.mainColorSet.color)
            rv.setImageViewBitmap(R.id.toolbar, color)

            val openIntent = Intent(context, MessengerAppWidgetProvider::class.java)
            openIntent.action = MessengerAppWidgetProvider.OPEN_ACTION
            openIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i])
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
            val openPendingIntent = PendingIntent.getBroadcast(context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            rv.setPendingIntentTemplate(R.id.widget_list, openPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetIds[i], rv)
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        private val TAG = "AppWidgetProvider"

        const val REFRESH_ACTION = "xyz.klinker.messenger.shared.widget.REFRESH"
        const val OPEN_ACTION = "xyz.klinker.messenger.shared.widget.OPEN"
        const val EXTRA_ITEM_ID = "xyz.klinker.messenger.shared.widget.EXTRA_ITEM_ID"

        var baseWidgetTheme = BaseTheme.DAY_NIGHT

        fun refreshWidget(context: Context) {
            try {
                val ids = AppWidgetManager.getInstance(context)
                        .getAppWidgetIds(ComponentName(context, MessengerAppWidgetProvider::class.java))

                val intent = Intent(context, MessengerAppWidgetProvider::class.java)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)

                context.sendBroadcast(intent)
                context.sendBroadcast(Intent(REFRESH_ACTION)) // send to dashclock
            } catch (e: Exception) {
            }
        }
    }

}
