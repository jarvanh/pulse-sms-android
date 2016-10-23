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

import xyz.klinker.messenger.data.DataSource;
import xyz.klinker.messenger.data.model.Blacklist;

/**
 * Helper for checking whether or not a contact is blacklisted.
 */
public class BlacklistUtils {

    public static boolean isBlacklisted(Context context, String number) {
        DataSource source = DataSource.getInstance(context);
        source.open();

        Cursor cursor = source.getBlacklists();
        if (cursor != null && cursor.moveToFirst()) {
            int numberIndex = cursor.getColumnIndex(Blacklist.COLUMN_PHONE_NUMBER);

            do {
                String blacklisted = cursor.getString(numberIndex);
                if (PhoneNumberUtils.checkEquality(number, blacklisted)) {
                    return true;
                }
            } while (cursor.moveToNext());
        }

        try {
            cursor.close();
        } catch (Exception e) { }

        return false;
    }

}
