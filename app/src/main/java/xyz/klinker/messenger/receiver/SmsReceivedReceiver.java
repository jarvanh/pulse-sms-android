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

package xyz.klinker.messenger.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.PhoneNumberUtil;
import xyz.klinker.messenger.util.SmsMmsUtil;

public class SmsReceivedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        String body = "";
        String address = "";
        long date = System.currentTimeMillis();
        Object[] smsExtra = (Object[]) extras.get("pdus");

        if (smsExtra == null) {
            return;
        }

        for (Object message : smsExtra) {
            SmsMessage sms;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String format = extras.getString("format");
                sms = SmsMessage.createFromPdu((byte[]) message, format);
            } else {
                sms = SmsMessage.createFromPdu((byte[]) message);
            }

            body += sms.getMessageBody();
            address = sms.getOriginatingAddress();
            date = sms.getTimestampMillis();
        }

        insertInternalSms(context, address, body, date);
        insertSms(context, address, body);
    }

    private void insertInternalSms(Context context, String address, String body, long dateSent) {
        ContentValues values = new ContentValues(5);
        values.put(Telephony.Sms.ADDRESS, address);
        values.put(Telephony.Sms.BODY, body);
        values.put(Telephony.Sms.DATE, System.currentTimeMillis());
        values.put(Telephony.Sms.READ, false);
        values.put(Telephony.Sms.DATE_SENT, dateSent);

        context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
    }

    private void insertSms(Context context, String address, String body) {
        Message message = new Message();
        message.type = Message.TYPE_RECEIVED;
        message.data = body;
        message.timestamp = System.currentTimeMillis();
        message.mimeType = MimeType.TEXT_PLAIN;
        message.read = false;
        message.seen = false;

        DataSource source = DataSource.getInstance(context);
        source.open();
        source.insertMessage(message, PhoneNumberUtil.clearFormatting(address), context);
        source.close();
    }

}
