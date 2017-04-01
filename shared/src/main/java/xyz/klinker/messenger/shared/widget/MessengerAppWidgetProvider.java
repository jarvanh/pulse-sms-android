/*
 * Copyright (C) 2016 Jacob Klinker
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

package xyz.klinker.messenger.shared.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.MessengerActivityExtras;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.util.ActivityUtils;
import xyz.klinker.messenger.shared.util.ImageUtils;

public class MessengerAppWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "AppWidgetProvider";

    public static final String REFRESH_ACTION = "xyz.klinker.messenger.shared.widget.REFRESH";
    public static final String OPEN_ACTION = "xyz.klinker.messenger.shared.widget.OPEN";
    public static final String EXTRA_ITEM_ID = "xyz.klinker.messenger.shared.widget.EXTRA_ITEM_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        superOnReceive(context, intent);

        AppWidgetManager mgr = getAppWidgetManager(context);

        String action = intent.getAction();
        if (action != null && action.equals(OPEN_ACTION)) {
            long itemId = intent.getLongExtra(EXTRA_ITEM_ID, 0);
            Intent openConversation = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY);
            openConversation.putExtra(MessengerActivityExtras.EXTRA_CONVERSATION_ID, itemId);
            openConversation.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(openConversation);
            return;
        }

        ComponentName thisAppWidget = new ComponentName(context.getPackageName(), MessengerAppWidgetProvider.class.getName());
        int[] appWidgetIds = mgr.getAppWidgetIds(thisAppWidget);

        try {
            mgr.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
        } catch (NullPointerException e) {
            Log.e(TAG, "failed to notify of widget changed", e);
        }
    }

    public AppWidgetManager getAppWidgetManager(Context context) {
        return AppWidgetManager.getInstance(context);
    }

    public void superOnReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int i = 0; i < appWidgetIds.length; ++i) {
            Intent intent = new Intent(context, MessengerWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

            Intent compose = ActivityUtils.buildForComponent(ActivityUtils.COMPOSE_ACTIVITY);
            PendingIntent pendingCompose = PendingIntent.getActivity(context, 0, compose, 0);

            Intent open = ActivityUtils.buildForComponent(ActivityUtils.MESSENGER_ACTIVITY);
            open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingOpen = PendingIntent.getActivity(context, 0, open, 0);

            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget);
            rv.setRemoteAdapter(R.id.widget_list, intent);
            rv.setOnClickPendingIntent(R.id.compose, pendingCompose);
            rv.setOnClickPendingIntent(R.id.title, pendingOpen);

            rv.setEmptyView(R.id.widget_list, R.id.widget_empty);

            Settings settings = Settings.get(context);
            if (settings.useGlobalThemeColor) {
                Bitmap color = ImageUtils.createColoredBitmap(settings.globalColorSet.color);
                rv.setImageViewBitmap(R.id.toolbar, color);
            }

            Intent openIntent = new Intent(context, MessengerAppWidgetProvider.class);
            openIntent.setAction(MessengerAppWidgetProvider.OPEN_ACTION);
            openIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            PendingIntent openPendingIntent = PendingIntent.getBroadcast(context, 0, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.widget_list, openPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public static void refreshWidget(Context context) {
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, MessengerAppWidgetProvider.class));

        Intent intent = new Intent(context, MessengerAppWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);

        context.sendBroadcast(intent);
        context.sendBroadcast(new Intent(REFRESH_ACTION)); // send to dashclock
    }

}
