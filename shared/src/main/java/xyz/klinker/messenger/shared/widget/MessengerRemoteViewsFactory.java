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

package xyz.klinker.messenger.shared.widget;

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

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.util.ContactUtils;
import xyz.klinker.messenger.shared.util.CursorUtil;
import xyz.klinker.messenger.shared.util.ImageUtils;

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
        reloadConversations();
    }

    private void reloadConversations() {
        Cursor items = DataSource.INSTANCE.getUnarchivedConversations(context);
        conversations = new ArrayList<>();

        if (items.moveToFirst()) {
            do {
                Conversation conversation = new Conversation();
                conversation.fillFromCursor(items);
                conversations.add(conversation);
            } while (items.moveToNext());
        }

        CursorUtil.closeSilent(items);
    }

    @Override
    public void onDataSetChanged() {
        reloadConversations();
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
        if (position >= conversations.size()) {
            return null;
        }

        Conversation item = conversations.get(position);

        if (item.getTitle() == null) {
            item.setTitle("");
        }

        if (item.getSnippet() == null) {
            item.setSnippet("");
        }

        if (!item.getRead()) {
            item.setTitle("<b>" + item.getTitle() + "</b>");
            item.setSnippet("<b>" + item.getSnippet() + "</b>");
        }

        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_item);

        Bitmap image = ImageUtils.getBitmap(context, item.getImageUri());
        if (image == null) {
            image = ImageUtils.createColoredBitmap(item.getColors().getColor());

            if (ContactUtils.shouldDisplayContactLetter(item)) {
                rv.setTextViewText(R.id.image_letter, item.getTitle().substring(0, 1));
            } else {
                rv.setTextViewText(R.id.image_letter, null);
            }
        } else {
            rv.setTextViewText(R.id.image_letter, null);
        }

        image = ImageUtils.clipToCircle(image);

        rv.setTextViewText(R.id.conversation_title, Html.fromHtml(item.getTitle()));
        rv.setTextViewText(R.id.conversation_summary, Html.fromHtml(item.getSnippet()));
        rv.setImageViewBitmap(R.id.picture, image);

        if (item.getRead()) {
            rv.setViewVisibility(R.id.unread_indicator, View.GONE);
        } else {
            rv.setViewVisibility(R.id.unread_indicator, View.VISIBLE);
        }

        Bundle extras = new Bundle();
        extras.putLong(MessengerAppWidgetProvider.EXTRA_ITEM_ID, item.getId());
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
        if (conversations.size() > 0 && position < conversations.size()) {
            return conversations.get(position).getId();
        } else {
            return 0;
        }
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

}
