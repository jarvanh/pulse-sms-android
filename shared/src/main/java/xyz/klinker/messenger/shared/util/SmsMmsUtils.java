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

package xyz.klinker.messenger.shared.util;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SqliteWrapper;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Telephony;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.transaction.MmsMessageSender;
import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.google.android.mms.pdu_alt.MultimediaMessagePdu;
import com.google.android.mms.pdu_alt.PduHeaders;
import com.google.android.mms.pdu_alt.PduPersister;
import com.google.android.mms.pdu_alt.RetrieveConf;
import com.klinker.android.send_message.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.IdMatcher;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.model.Conversation;
import xyz.klinker.messenger.shared.data.model.Message;

public class SmsMmsUtils {

    private static final String TAG = "SmsMmsUtils";

    public static final int INITIAL_CONVERSATION_LIMIT = 500;
    public static final int INITIAL_MESSAGE_LIMIT = 500;

    /**
     * Gets a list of conversations from the internal sms database that is ready to be inserted
     * into our database.
     *
     * @param context the current application context.
     * @return a list of conversations that is filled and ready to be inserted into our database.
     */
    public static List<Conversation> queryConversations(Context context) {
        List<Conversation> conversations = new ArrayList<>();

        String[] projection = new String[]{
                Telephony.ThreadsColumns._ID,
                Telephony.ThreadsColumns.DATE,
                Telephony.ThreadsColumns.MESSAGE_COUNT,
                Telephony.ThreadsColumns.RECIPIENT_IDS,
                Telephony.ThreadsColumns.SNIPPET,
                Telephony.ThreadsColumns.READ
        };

        Uri uri = Uri.parse(Telephony.Threads.CONTENT_URI.toString() + "?simple=true");

        Cursor cursor;

        try {
            cursor = context.getContentResolver()
                    .query(uri, projection, null, null, Telephony.ThreadsColumns.DATE + " desc");
        } catch (SQLException | SecurityException e) {
            cursor = null;
        }

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Conversation conversation = new Conversation();
                conversation.id = cursor.getLong(0);
                conversation.pinned = false;
                conversation.read = cursor.getInt(5) == 1;
                conversation.timestamp = cursor.getLong(1);
                conversation.snippet = cursor.getString(4);
                conversation.ringtoneUri = null;
                conversation.phoneNumbers = ContactUtils.findContactNumbers(cursor.getString(3), context);
                conversation.title = ContactUtils.findContactNames(conversation.phoneNumbers, context);
                conversation.imageUri = ContactUtils.findImageUri(conversation.phoneNumbers, context);
                conversation.idMatcher = createIdMatcher(conversation.phoneNumbers).getDefault();
                conversation.mute = false;
                conversation.privateNotifications = false;
                conversation.ledColor = Color.WHITE;
                ImageUtils.fillConversationColors(conversation, context);
                conversation.simSubscriptionId = -1;

                conversations.add(conversation);
            } while (cursor.moveToNext() && conversations.size() < INITIAL_CONVERSATION_LIMIT);
        }

        try {
            cursor.close();
        } catch (Exception e) { }

        return conversations;
    }

    /**
     * Creates a column that we can use later on for a findOrCreateConversationId method on my
     * database. It will take all of the comma, space separated numbers and combine them together
     * by taking the last 5  (and 7) digits of each number, sorting them and then recombining them into a
     * single string. We can then do the same process for any string of phone numbers later on
     * and search for that string in the data source to see if it exists yet.
     *
     * I added the seven digit finder after some issues that people ran into with conversations not
     * being able to be saved correctly. It is now being used throughout the app, but I needed
     * to continue supporting the legacy version (5 digits) as well.
     *
     * @param phoneNumbers the phone numbers to look for.
     * @return the combined string.
     */
    public static IdMatcher createIdMatcher(String phoneNumbers) {
        String[] numbers = phoneNumbers.split(", ");

        List<String> fiveMatchers = new ArrayList<>();
        List<String> sevenMatchers = new ArrayList<>();
        List<String> sevenMatchersNoFormatting = new ArrayList<>();
        List<String> eightMatchers = new ArrayList<>();
        List<String> tenMatchers = new ArrayList<>();

        for (String n : numbers) {
            n = n.replaceAll("-","").replaceAll(" ", "").replaceAll("/+", "");
            if (n.contains("@")) {
                fiveMatchers.add(n);
            } else if (n.length() >= 5) {
                fiveMatchers.add(n.substring(n.length() - 5));
            } else {
                fiveMatchers.add(n);
            }
        }

        for (String n : numbers) {
            n = n.replaceAll("-","").replaceAll(" ", "").replaceAll("/+", "");
            if (n.contains("@")) {
                sevenMatchers.add(n);
            } else if (n.length() >= 7) {
                sevenMatchers.add(n.substring(n.length() - 7));
            } else {
                sevenMatchers.add(n);
            }
        }

        for (String n : numbers) {
            n = PhoneNumberUtils.clearFormatting(n);
            n = n.replaceAll("-","").replaceAll(" ", "").replaceAll("/+", "");
            if (n.contains("@")) {
                sevenMatchersNoFormatting.add(n);
            } else if (n.length() >= 7) {
                sevenMatchersNoFormatting.add(n.substring(n.length() - 7));
            } else {
                sevenMatchersNoFormatting.add(n);
            }
        }

        for (String n : numbers) {
            n = n.replaceAll("-","").replaceAll(" ", "").replaceAll("/+", "");
            if (n.contains("@")) {
                eightMatchers.add(n);
            } else if (n.length() >= 8) {
                eightMatchers.add(n.substring(n.length() - 8));
            } else {
                eightMatchers.add(n);
            }
        }

        for (String n : numbers) {
            n = n.replaceAll("-","").replaceAll(" ", "").replaceAll("/+", "");
            if (n.contains("@")) {
                tenMatchers.add(n);
            } else if (n.length() >= 10) {
                tenMatchers.add(n.substring(n.length() - 10));
            } else {
                tenMatchers.add(n);
            }
        }

        Collections.sort(fiveMatchers);
        Collections.sort(sevenMatchers);
        Collections.sort(sevenMatchersNoFormatting);
        Collections.sort(eightMatchers);
        Collections.sort(tenMatchers);

        StringBuilder tenBuilder = new StringBuilder();
        for (String m : tenMatchers) {
            tenBuilder.append(m);
        }

        StringBuilder sevenBuilder = new StringBuilder();
        for (String m : sevenMatchers) {
            sevenBuilder.append(m);
        }

        StringBuilder sevenNoFormattingBuilder = new StringBuilder();
        for (String m : sevenMatchersNoFormatting) {
            sevenNoFormattingBuilder.append(m);
        }

        StringBuilder eightBuilder = new StringBuilder();
        for (String m : eightMatchers) {
            eightBuilder.append(m);
        }

        StringBuilder fiveBuilder = new StringBuilder();
        for (String m : fiveMatchers) {
            fiveBuilder.append(m);
        }

        return new IdMatcher(fiveBuilder.toString(), sevenBuilder.toString(), sevenNoFormattingBuilder.toString(), eightBuilder.toString(), tenBuilder.toString());
    }

    /**
     * Queries a conversation that is currently in the database and returns a cursor with all of the
     * data.
     *
     * @param conversationId the internal sms db conversation id.
     * @return the conversation as a cursor.
     */
    public static Cursor queryConversation(long conversationId, Context context) {
        if (conversationId == -1) {
            return null;
        }

        String[] projection = new String[]{
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
        String sortOrder = "normalized_date desc";

        return context.getContentResolver().query(uri, projection, null, null, sortOrder);
    }

    /**
     * Gets content values that can be inserted into our own database from a cursor with data from
     * the internal database. See queryConversation(). For an mms message, there could be multiple
     * messages that need to be inserted, so the method returns a list.
     *
     * @param messages       the cursor holding the message.
     * @param conversationId the conversation id from our own internal database.
     * @return the content values to insert into our database.
     */
    public static List<ContentValues> processMessage(Cursor messages, long conversationId,
                                                     Context context) {
        List<ContentValues> values = new ArrayList<>();

        if (isSms(messages)) {
            if (messages.getString(1) != null) {
                ContentValues message = new ContentValues(9);
                message.put(Message.COLUMN_ID, DataSource.INSTANCE.generateId());
                message.put(Message.COLUMN_CONVERSATION_ID, conversationId);
                message.put(Message.COLUMN_TYPE, getSmsMessageType(messages));
                message.put(Message.COLUMN_DATA, messages.getString(1).trim());
                message.put(Message.COLUMN_TIMESTAMP, messages.getLong(2));
                message.put(Message.COLUMN_MIME_TYPE, MimeType.TEXT_PLAIN);
                message.put(Message.COLUMN_READ, messages.getInt(3));
                message.put(Message.COLUMN_SEEN, true);
                message.put(Message.COLUMN_FROM, (String) null);
                message.put(Message.COLUMN_COLOR, (Integer) null);

                values.add(message);
            }
        } else {
            Uri uri = Uri.parse("content://mms/" + messages.getLong(0));
            final String number = getMmsFrom(uri, context);
            final String from = ContactUtils.findContactNames(number, context);
            final String mId = "mid=" + messages.getString(0);
            int type = getMmsMessageType(messages);

            Cursor query = context.getContentResolver().query(Uri.parse("content://mms/part"),
                    new String[]{
                            Telephony.Mms.Part._ID,
                            Telephony.Mms.Part.CONTENT_TYPE,
                            Telephony.Mms.Part._DATA,
                            Telephony.Mms.Part.TEXT
                    },
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
                        message.put(Message.COLUMN_TIMESTAMP, messages
                                .getLong(messages.getColumnIndex(Telephony.Sms.DATE)) * 1000);
                        message.put(Message.COLUMN_READ, messages
                                .getInt(messages.getColumnIndex(Telephony.Sms.READ)));
                        message.put(Message.COLUMN_SEEN, true);
                        message.put(Message.COLUMN_FROM, from);
                        message.put(Message.COLUMN_COLOR, (Integer) null);

                        if (mimeType.equals(MimeType.TEXT_PLAIN)) {
                            String data = query.getString(2);
                            String text;
                            if (data != null) {
                                text = getMmsText(partId, context);
                            } else {
                                text = query.getString(3);
                            }

                            if (text == null) {
                                text = "";
                            }

                            if (text.trim().length() != 0) {
                                message.put(Message.COLUMN_DATA, text.trim());
                                values.add(message);
                            }
                        } else {
                            message.put(Message.COLUMN_DATA, "content://mms/part/" + partId);
                            values.add(message);
                        }
                    }
                } while (query.moveToNext());
            }

            try {
                query.close();
            } catch (Exception e) { }
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
        return message.getString(message.getColumnIndex(Telephony.Mms.MESSAGE_BOX)) == null;
    }

    /**
     * Gets the message type of the internal sms. It will be one of the constants defined in Message,
     * eg TYPE_RECEIVED, TYPE_SENT, etc.
     *
     * @param message the message to inspect.
     * @return the Message.TYPE_ value.
     */
    public static int getSmsMessageType(Cursor message) {
        int internalType = message.getInt(message.getColumnIndex(Telephony.Sms.TYPE));
        int status = message.getInt(message.getColumnIndex(Telephony.Sms.STATUS));

        if (status == Telephony.Sms.STATUS_NONE ||
                internalType == Telephony.Sms.MESSAGE_TYPE_INBOX) {
            if (internalType == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                return Message.TYPE_RECEIVED;
            } else if (internalType == Telephony.Sms.MESSAGE_TYPE_FAILED) {
                return Message.TYPE_ERROR;
            } else if (internalType == Telephony.Sms.MESSAGE_TYPE_OUTBOX) {
                return Message.TYPE_SENDING;
            } else if (internalType == Telephony.Sms.MESSAGE_TYPE_SENT) {
                return Message.TYPE_SENT;
            } else {
                return Message.TYPE_SENT;
            }
        } else {
            if (status == Telephony.Sms.STATUS_COMPLETE) {
                return Message.TYPE_DELIVERED;
            } else if (status == Telephony.Sms.STATUS_PENDING) {
                return Message.TYPE_SENT;
            } else if (status == Telephony.Sms.STATUS_FAILED) {
                return Message.TYPE_ERROR;
            } else {
                return Message.TYPE_SENT;
            }
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
        int internalType = message.getInt(message.getColumnIndex(Telephony.Mms.MESSAGE_BOX));

        if (internalType == Telephony.Mms.MESSAGE_BOX_INBOX) {
            return Message.TYPE_RECEIVED;
        } else if (internalType == Telephony.Mms.MESSAGE_BOX_FAILED) {
            return Message.TYPE_ERROR;
        } else if (internalType == Telephony.Mms.MESSAGE_BOX_OUTBOX) {
            return Message.TYPE_SENDING;
        } else if (internalType == Telephony.Mms.MESSAGE_BOX_SENT) {
            return Message.TYPE_SENT;
        } else {
            return Message.TYPE_SENT;
        }
    }

    public static String getMmsFrom(Uri uri, Context context) {
        String msgId = uri.getLastPathSegment();
        Uri.Builder builder = Telephony.Mms.CONTENT_URI.buildUpon();

        builder.appendPath(msgId).appendPath("addr");

        Cursor cursor = SqliteWrapper.query(context, context.getContentResolver(),
                builder.build(), new String[]{Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.CHARSET},
                Telephony.Mms.Addr.TYPE + "=" + PduHeaders.FROM, null, null);

        if (cursor != null) {
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
        }

        try {
            cursor.close();
        } catch (Exception e) { }

        return "";
    }

    public static String getMmsTo(Uri uri, Context context) {
        MultimediaMessagePdu msg;

        try {
            msg = (MultimediaMessagePdu) PduPersister.getPduPersister(
                    context).load(uri);
        } catch (Exception e) {
            return "";
        }

        StringBuilder toBuilder = new StringBuilder();
        EncodedStringValue[] to = msg.getTo();

        if (to != null) {
            toBuilder.append(EncodedStringValue.concat(to));
        }

        if (msg instanceof RetrieveConf) {
            EncodedStringValue[] cc = ((RetrieveConf) msg).getCc();
            if (cc != null && cc.length > 0) {
                toBuilder.append(";");
                toBuilder.append(EncodedStringValue.concat(cc));
            }
        }

        String built = toBuilder.toString().replace(";", ", ");
        if (built.startsWith(", ")) {
            built = built.substring(2);
        }

        return stripDuplicatePhoneNumbers(built);
    }

    /**
     * Expects the conversation formatted list of phone numbers and returns the same list,
     * stripped of duplicates.
     *
     * @param phoneNumbers comma and space separated list of numbers.
     * @return the same list, with any duplicates stripped out.
     */
    public static String stripDuplicatePhoneNumbers(String phoneNumbers) {
        if (phoneNumbers == null) {
            return "";
        }

        String[] split = phoneNumbers.split(", ");
        Set<String> numbers = new HashSet<>();

        for (String s : split) {
            numbers.add(s);
        }

        StringBuilder builder = new StringBuilder();
        for (String s : numbers) {
            builder.append(s);
            builder.append(", ");
        }

        String result = builder.toString();
        if (result.contains(", ")) {
            result = result.substring(0, result.length() - 2);
        }

        return result;
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

    /**
     * Gets the last sms message that was inserted into the database.
     *
     * @param context the context to get the content provider with.
     * @return the cursor for a single mms message.
     */
    public static Cursor getLastSmsMessage(Context context) {
        Uri uri = Uri.parse("content://sms");
        String sortOrder = "date desc limit 1";
        return getSmsMessage(context, uri, sortOrder);
    }

    /**
     * Gets the last sms message that was inserted into the database.
     *
     * @param context the context to get the content provider with.
     * @return the cursor for a single mms message.
     */
    public static Cursor getLatestSmsMessages(Context context, int limit) {
        Uri uri = Uri.parse("content://sms");
        String sortOrder = "date desc limit " + limit;
        return getSmsMessage(context, uri, sortOrder);
    }

    /**
     * Get an SMS message(s) from the provided URI.
     *
     * @param context   the context for the content provider.
     * @param uri       the sms message uri.
     * @param sortOrder the sort order to apply.
     * @return the cursor for the messages that match.
     */
    public static Cursor getSmsMessage(Context context, Uri uri, String sortOrder) {
        String[] projection = new String[]{
                Telephony.MmsSms._ID,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
                Telephony.Sms.TYPE,
                Telephony.Sms.STATUS,
                Telephony.Sms.ADDRESS
        };

        try {
            return context.getContentResolver().query(uri, projection, null, null, sortOrder);
        } catch (Exception e) {
            // they probably aren't using our app as the default?
            return null;
        }
    }

    /**
     * Gets the last mms message that was inserted into the database.
     *
     * @param context the context to get the content provider with.
     * @return the cursor for a single mms message.
     */
    public static Cursor getLastMmsMessage(Context context) {
        Uri uri = Uri.parse("content://mms");
        String sortOrder = "date desc limit 1";
        return getMmsMessage(context, uri, sortOrder);
    }

    /**
     * Get an MMS message(s) from the provided URI.
     *
     * @param context   the context for the content provider.
     * @param uri       the mms message uri.
     * @param sortOrder the sort order to apply.
     * @return the cursor for the messages that match.
     */
    public static Cursor getMmsMessage(Context context, Uri uri, String sortOrder) {
        String[] projection = new String[]{
                Telephony.MmsSms._ID,
                Telephony.Sms.DATE,
                Telephony.Sms.READ,
                Telephony.Mms.MESSAGE_BOX,
                Telephony.Mms.MESSAGE_TYPE
        };

        return context.getContentResolver().query(uri, projection, null, null, sortOrder);
    }

    /**
     * Marks a conversation as read in the internal database.
     *
     * @param context      the context to get the content provider with.
     * @param phoneNumbers the phone numbers to find the conversation with.
     */
    public static void markConversationRead(final Context context, final String phoneNumbers) {
        new Thread(() -> {
            try {
                Set<String> recipients = new HashSet<>();
                Collections.addAll(recipients, phoneNumbers.split(", "));
                long threadId = Utils.getOrCreateThreadId(context, recipients);
                markConversationRead(context,
                        ContentUris.withAppendedId(Telephony.Threads.CONTENT_URI, threadId), threadId);
            } catch (IllegalStateException | IllegalArgumentException | SQLException | SecurityException e) {
                // the conversation doesn't exist
                e.printStackTrace();
            }
        }).start();
    }

    private static void markConversationRead(final Context context, final Uri threadUri,
                                             long threadId) {
        Log.v(TAG, "marking thread as read. Thread Id: " + threadId + ", Thread Uri: " + threadUri);

        // If we have no Uri to mark (as in the case of a conversation that
        // has not yet made its way to disk), there's nothing to do.
        if (threadUri != null) {
            // Check the read flag first. It's much faster to do a query than
            // to do an update. Timing this function show it's about 10x faster to
            // do the query compared to the update, even when there's nothing to
            // update.
            boolean needUpdate = true;

            Cursor c = context.getContentResolver().query(threadUri,
                    new String[]{"_id", "read", "seen"}, "(read=0 OR seen=0)", null, null);
            if (c != null) {
                try {
                    needUpdate = c.getCount() > 0;
                } finally {
                    c.close();
                }
            }

            if (needUpdate) {
                Log.v(TAG, "MMS need to be marked as read");

                ContentValues values = new ContentValues(2);
                values.put("read", 1);
                values.put("seen", 1);

                sendReadReport(context, threadId, PduHeaders.READ_STATUS_READ);
                context.getContentResolver().update(threadUri, values,
                        "(read=0 OR seen=0)", null);
            }
        }
    }

    private static void sendReadReport(final Context context,
                                       final long threadId,
                                       final int status) {
//        String selection = Telephony.Mms.MESSAGE_TYPE + " = " + PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF
//                + " AND " + Telephony.Mms.READ + " = 0"
//                + " AND " + Telephony.Mms.READ_REPORT + " = " + PduHeaders.VALUE_YES;
        String selection = Telephony.Mms.READ + " = 0";

        if (threadId != -1) {
            selection = selection + " AND " + Telephony.Mms.THREAD_ID + " = " + threadId;
        }

        try {
            final Cursor c = context.getContentResolver().query(Telephony.Mms.Inbox.CONTENT_URI,
                    new String[]{Telephony.Mms._ID, Telephony.Mms.MESSAGE_ID},
                    selection, null, null);

            if (c != null && c.moveToFirst()) {
                do {
                    Log.v("SmsMmsUtils", "marking MMS as seen. ID:" + c.getString(1));
                    Uri uri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, c.getLong(0));
                    MmsMessageSender.sendReadRec(context, getMmsFrom(uri, context),
                            c.getString(1), status);
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a conversation from the internal sms database.
     */
    public static void deleteConversation(Context context, String phoneNumbers) {
        try {
            Set<String> recipients = new HashSet<>();
            Collections.addAll(recipients, phoneNumbers.split(", "));
            long threadId = Utils.getOrCreateThreadId(context, recipients);
            context.getContentResolver().delete(Uri.parse("content://mms-sms/conversations/" +
                    threadId + "/"), null, null);
            context.getContentResolver().delete(Uri.parse("content://mms-sms/conversations/"),
                    "_id=?", new String[]{Long.toString(threadId)});
        } catch (Exception e) {
            Log.e("delete conversation", "error deleting", e);
        }
    }

}
