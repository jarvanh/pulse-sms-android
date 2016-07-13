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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.data.ColorSet;
import xyz.klinker.messenger.data.model.Conversation;
import xyz.klinker.messenger.data.model.Message;

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
                conversation.id = cursor.getLong(0);
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
                    Bitmap bitmap = ImageUtil.getContactImage(conversation.imageUri, context);
                    ColorSet colors = ImageUtil.extractColorSet(context, bitmap);

                    if (colors != null) {
                        conversation.colors = colors;
                        conversation.imageUri = Uri
                                .withAppendedPath(Uri.parse(conversation.imageUri),
                                        ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
                                .toString();
                    } else {
                        conversation.colors = ColorUtil.getRandomMaterialColor(context);
                        conversation.imageUri = null;
                    }
                }

                conversations.add(conversation);
            } while (cursor.moveToNext());

            cursor.close();
        }

        return conversations;
    }

    /**
     * Queries a conversation that is currently in the database and returns a cursor with all of the
     * data.
     *
     * @param conversationId the internal sms db conversation id.
     * @return the conversation as a cursor.
     */
    public static Cursor queryConversation(long conversationId, Context context) {
        String[] projection = new String[] {
                Telephony.MmsSms._ID,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
                Telephony.Sms.TYPE,
                Telephony.Mms.MESSAGE_BOX,
                Telephony.Mms.MESSAGE_TYPE,
                Telephony.Sms.STATUS
        };

        Uri uri = Uri.parse("content://mms-sms/conversations/" + conversationId + "/");
        String sortOrder = "normalized_date";

        return context.getContentResolver().query(uri, projection, null, null, sortOrder);
    }

    /**
     * Gets content values that can be inserted into our own database from a cursor with data from
     * the internal database. See queryConversation().
     *
     * @param messages the cursor holding the message.
     * @param conversationId the conversation id from our own internal database.
     * @return the content values to insert into our database.
     */
    public static ContentValues processMessage(Cursor messages, long conversationId) {
        if (isSms(messages)) {
            ContentValues message = new ContentValues(9);
            message.put(Message.COLUMN_CONVERSATION_ID, conversationId);
            message.put(Message.COLUMN_TYPE, getMessageType(messages));
            message.put(Message.COLUMN_DATA, messages.getString(1));
            message.put(Message.COLUMN_TIMESTAMP, messages.getLong(2));
            message.put(Message.COLUMN_MIME_TYPE, "text/plain");
            message.put(Message.COLUMN_READ, messages.getInt(3));
            message.put(Message.COLUMN_SEEN, true);
            message.put(Message.COLUMN_FROM, (String) null);
            message.put(Message.COLUMN_COLOR, (Integer) null);

            return message;
        } else {
            // TODO process MMS here
            return null;
        }
    }

    /**
     * Checks whether or not the msg_box column has data in it. If it doesn't, then the message is
     * SMS. If it does, the message is MMS.
     *
     * @param message the message to try.
     * @return true for sms, false for mms.
     */
    private static boolean isSms(Cursor message) {
        return message.getString(5) == null;
    }

    /**
     * Gets the message type of the internal sms. It will be one of the constants defined in Message,
     * eg TYPE_RECEIVED, TYPE_SENT, etc.
     *
     * @param message the message to inspect.
     * @return the Message.TYPE_ value.
     */
    private static int getMessageType(Cursor message) {
        int type = message.getInt(4);

        if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
            return Message.TYPE_RECEIVED;
        } else if (type == Telephony.Sms.MESSAGE_TYPE_FAILED) {
            return Message.TYPE_ERROR;
        } else if (type == Telephony.Sms.MESSAGE_TYPE_OUTBOX) {
            return Message.TYPE_SENDING;
        } else if (type == Telephony.Sms.MESSAGE_TYPE_SENT) {
            return Message.TYPE_SENT;
        } else {
            // TODO process delivery status here
            return Message.TYPE_SENT;
        }
    }

}
