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
import android.database.sqlite.SqliteWrapper;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.text.TextUtils;

import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduPersister;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.data.ColorSet;
import xyz.klinker.messenger.data.MimeType;
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
     * the internal database. See queryConversation(). For an mms message, there could be multiple
     * messages that need to be inserted, so the method returns a list.
     *
     * @param messages the cursor holding the message.
     * @param conversationId the conversation id from our own internal database.
     * @return the content values to insert into our database.
     */
    public static List<ContentValues> processMessage(Cursor messages, long conversationId,
                                                     Context context) {
        List<ContentValues> values = new ArrayList<>();

        if (isSms(messages)) {
            ContentValues message = new ContentValues(9);
            message.put(Message.COLUMN_CONVERSATION_ID, conversationId);
            message.put(Message.COLUMN_TYPE, getSmsMessageType(messages));
            message.put(Message.COLUMN_DATA, messages.getString(1));
            message.put(Message.COLUMN_TIMESTAMP, messages.getLong(2));
            message.put(Message.COLUMN_MIME_TYPE, "text/plain");
            message.put(Message.COLUMN_READ, messages.getInt(3));
            message.put(Message.COLUMN_SEEN, true);
            message.put(Message.COLUMN_FROM, (String) null);
            message.put(Message.COLUMN_COLOR, (Integer) null);

            values.add(message);
        } else {
            Uri uri = Uri.parse("content://mms/" + messages.getLong(0));
            final String number = getMmsFrom(uri, context);
            final String from = ContactUtil.findContactNames(number, context);
            final String mId = "mid=" + messages.getString(0);
            int type = getMmsMessageType(messages);

            Cursor query = context.getContentResolver().query(Uri.parse("content://mms/part"),
                    new String[]{"_id", "ct", "_data", "text"},
                    mId,
                    null,
                    null);

            if (query != null && query.moveToFirst()) {
                do {
                    String partId = query.getString(0);
                    String mimeType = query.getString(1);

                    if (mimeType != null && MimeType.isSupported(mimeType)) {
                        ContentValues message = new ContentValues(9);
                        message.put(Message.COLUMN_CONVERSATION_ID, conversationId);
                        message.put(Message.COLUMN_TYPE, type);
                        message.put(Message.COLUMN_MIME_TYPE, mimeType);
                        message.put(Message.COLUMN_TIMESTAMP, messages.getLong(2) * 1000);
                        message.put(Message.COLUMN_READ, messages.getInt(3));
                        message.put(Message.COLUMN_SEEN, true);
                        message.put(Message.COLUMN_FROM, from);
                        message.put(Message.COLUMN_COLOR, (Integer) null);

                        if (mimeType.equals("text/plain")) {
                            String data = query.getString(2);
                            String text;
                            if (data != null) {
                                text = getMmsText(partId, context);
                            } else {
                                text = query.getString(3);
                            }

                            if (text.trim().length() != 0) {
                                message.put(Message.COLUMN_DATA, text);
                                values.add(message);
                            }
                        } else {
                            message.put(Message.COLUMN_DATA, "content://mms/part/" + partId);
                            values.add(message);
                        }
                    }
                } while (query.moveToNext());

                query.close();
            }
        }

        return values;
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
    private static int getSmsMessageType(Cursor message) {
        int internalType = message.getInt(4);

        if (internalType == Telephony.Sms.MESSAGE_TYPE_INBOX) {
            return Message.TYPE_RECEIVED;
        } else if (internalType == Telephony.Sms.MESSAGE_TYPE_FAILED) {
            return Message.TYPE_ERROR;
        } else if (internalType == Telephony.Sms.MESSAGE_TYPE_OUTBOX) {
            return Message.TYPE_SENDING;
        } else if (internalType == Telephony.Sms.MESSAGE_TYPE_SENT) {
            return Message.TYPE_SENT;
        } else {
            // TODO process delivery status here
            return Message.TYPE_SENT;
        }
    }

    /**
     * Gets the message type of the internal mms. It will be one of the constants defined in Message,
     * eg TYPE_RECEIVED, TYPE_SENT, etc.
     *
     * @param message the message to inspect.
     * @return the Message.TYPE_ value.
     */
    private static int getMmsMessageType(Cursor message) {
        int internalType = message.getInt(5);

        if (internalType == Telephony.Mms.MESSAGE_BOX_INBOX) {
            return Message.TYPE_RECEIVED;
        } else if (internalType == Telephony.Mms.MESSAGE_BOX_FAILED) {
            return Message.TYPE_ERROR;
        } else if (internalType == Telephony.Mms.MESSAGE_BOX_OUTBOX) {
            return Message.TYPE_SENDING;
        } else if (internalType == Telephony.Mms.MESSAGE_BOX_SENT) {
            return Message.TYPE_SENT;
        } else {
            // TODO process delivery status here
            return Message.TYPE_SENT;
        }
    }

    private static String getMmsFrom(Uri uri, Context context) {
        String msgId = uri.getLastPathSegment();
        Uri.Builder builder = Telephony.Mms.CONTENT_URI.buildUpon();

        builder.appendPath(msgId).appendPath("addr");

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                builder.build(), new String[]{Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.CHARSET},
                Telephony.Mms.Addr.TYPE + "=" + PduHeaders.FROM, null, null);

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String from = cursor.getString(0);

                    if (!TextUtils.isEmpty(from)) {
                        byte[] bytes = PduPersister.getBytes(from);
                        int charset = cursor.getInt(1);
                        cursor.close();
                        return new EncodedStringValue(charset, bytes)
                                .getString();
                    }
                }
            } finally {
                cursor.close();
            }
        }

        return "";
    }

    private static String getMmsText(String id, Context context) {
        Uri partURI = Uri.parse("content://mms/part/" + id);
        InputStream is = null;
        StringBuilder sb = new StringBuilder();
        try {
            is = context.getContentResolver().openInputStream(partURI);
            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                String temp = reader.readLine();
                while (temp != null) {
                    sb.append(temp);
                    temp = reader.readLine();
                }
            }
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return sb.toString();
    }

}
