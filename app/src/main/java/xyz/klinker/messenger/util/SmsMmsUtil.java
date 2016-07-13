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

package xyz.klinker.messenger.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.data.model.Conversation;

public class SmsMmsUtil {

    /**
     * Gets a list of conversations from the internal sms database that is ready to be inserted
     * into our database.
     *
     * @param context the current application context.
     * @return a list of conversations that is filled and ready to be inserted into our database.
     */
    public static List<Conversation> queryConversations(Context context) {
        List<Conversation> conversations = new ArrayList<>();

        String[] projection = new String[] {
                Telephony.ThreadsColumns._ID,
                Telephony.ThreadsColumns.DATE,
                Telephony.ThreadsColumns.MESSAGE_COUNT,
                Telephony.ThreadsColumns.RECIPIENT_IDS,
                Telephony.ThreadsColumns.SNIPPET,
                Telephony.ThreadsColumns.READ
        };

        Uri uri = Uri.parse(Telephony.Threads.CONTENT_URI.toString() + "?simple=true");

        Cursor cursor = context.getContentResolver()
                .query(uri, projection, null, null, Telephony.ThreadsColumns.DATE + " desc");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Conversation conversation = new Conversation();
                conversation.pinned = false;
                conversation.read = cursor.getInt(5) == 1;
                conversation.timestamp = cursor.getLong(1);
                conversation.snippet = cursor.getString(4);
                conversation.ringtoneUri = null;
                conversation.phoneNumbers = ContactUtil.findContactNumbers(cursor.getString(3), context);
                conversation.title = ContactUtil.findContactNames(conversation.phoneNumbers, context);
                conversation.imageUri = ContactUtil.findImageUri(conversation.phoneNumbers, context);

                if (conversation.imageUri == null) {
                    conversation.colors = ColorUtil.getRandomMaterialColor(context);
                } else {
                    conversation.colors = ImageUtil
                            .extractContactColorSet(context, conversation.imageUri);
                }

                conversations.add(conversation);
            } while (cursor.moveToNext());

            cursor.close();
        }

        return conversations;
    }


}
