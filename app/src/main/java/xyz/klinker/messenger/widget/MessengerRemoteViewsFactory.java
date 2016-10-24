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

package xyz.klinker.messenger.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.util.ContactUtils;
import xyz.klinker.messenger.util.ImageUtils;

public class MessengerRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Settings settings;
    private Context context;
    private List<Conversation> conversations;

    public MessengerRemoteViewsFactory(Context context) {
        this.context = context;
        this.settings = Settings.get(context);
    }

    @Override
    public void onCreate() {
        reloadArticles();
    }

    private void reloadArticles() {
        DataSource source = DataSource.getInstance(context);
        source.open();
        Cursor items = source.getUnarchivedConversations();
        conversations = new ArrayList<>();

        if (items.moveToFirst()) {
            do {
                Conversation conversation = new Conversation();
                conversation.fillFromCursor(items);
                conversations.add(conversation);
            } while (items.moveToNext());
        }

        try {
            items.close();
        } catch (Exception e) { }

        source.close();
    }

    @Override
    public void onDataSetChanged() {
        reloadArticles();
    }

    @Override
    public void onDestroy() {
        conversations.clear();
    }

    @Override
    public int getCount() {
        return conversations.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Conversation item = conversations.get(position);

        if (item.title == null) {
            item.title = "";
        }

        if (item.snippet == null) {
            item.snippet = "";
        }

        if (!item.read) {
            item.title = "<b>" + item.title + "</b>";
            item.snippet = "<b>" + item.snippet + "</b>";
        }

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_item);

        Bitmap image = ImageUtils.getBitmap(context, item.imageUri);
        if (image == null) {
            image = ImageUtils.createColoredBitmap(item.colors.color);

            if (ContactUtils.shouldDisplayContactLetter(item)) {
                rv.setTextViewText(R.id.image_letter, item.title.substring(0, 1));
            } else {
                rv.setTextViewText(R.id.image_letter, null);
            }
        } else {
            rv.setTextViewText(R.id.image_letter, null);
        }

        image = ImageUtils.clipToCircle(image);

        rv.setTextViewText(R.id.conversation_title, Html.fromHtml(item.title));
        rv.setTextViewText(R.id.conversation_summary, Html.fromHtml(item.snippet));
        rv.setImageViewBitmap(R.id.picture, image);

        if (item.read) {
            rv.setViewVisibility(R.id.unread_indicator, View.GONE);
        } else {
            rv.setViewVisibility(R.id.unread_indicator, View.VISIBLE);
        }

        Bundle extras = new Bundle();
        extras.putLong(MessengerAppWidgetProvider.EXTRA_ITEM_ID, item.id);
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return conversations.get(position).id;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

}
