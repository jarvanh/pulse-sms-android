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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Helper for working with Android's contact provider.
 */
public class ContactUtil {

    /**
     * Gets a space separated list of phone numbers.
     *
     * @param recipientIds the internal sms database recipient ids.
     * @param context the application context.
     * @return the comma and space separated list of numbers.
     */
    public static String findContactNumbers(String recipientIds, Context context) {
        try {
            String[] ids = recipientIds.split(" ");
            List<String> numbers = new ArrayList<>();

            for (int i = 0; i < ids.length; i++) {
                try {
                    if (ids[i] != null && (!ids[i].equals("") || !ids[i].equals(" "))) {
                        Cursor number = context.getContentResolver()
                                .query(Uri.parse("content://mms-sms/canonical-addresses"), null,
                                        "_id=?", new String[] { ids[i] }, null);

                        if (number != null && number.moveToFirst()) {
                            numbers.add(PhoneNumberUtil.clearFormatting(
                                    number.getString(number.getColumnIndex("address"))));
                        } else {
                            numbers.add(ids[i]);
                        }

                        number.close();
                    }
                } catch (Exception e) {
                    numbers.add("0");
                }
            }

            Collections.sort(numbers);

            StringBuilder number = new StringBuilder();
            for (String n : numbers) {
                number.append(", ");
                number.append(n);
            }

            return number.toString().substring(2);
        } catch (Exception e) {
            return recipientIds;
        }
    }

    /**
     * Gets a comma and space separated list of names from numbers.
     *
     * @param numbers the comma and space separated list of phone numbers to look up.
     * @param context the current application context.
     * @return a space separated list of names.
     */
    public static String findContactNames(String numbers, Context context) {
        String names = "";
        String[] number;

        try {
            number = numbers.split(", ");
        } catch (Exception e) {
            return "";
        }

        for (int i = 0; i < number.length; i++) {
            try {
                String origin = number[i];

                Uri phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(origin));

                Cursor phonesCursor = null;

                try {
                    phonesCursor = context.getContentResolver()
                            .query(
                                    phoneUri,
                                    new String[] {
                                        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                                        ContactsContract.RawContacts._ID
                                    },
                                    null,
                                    null,
                                    ContactsContract.Contacts.DISPLAY_NAME + " desc limit 1");

                } catch (Exception e) {
                    // funky placeholder number coming from an mms message, dont do anything with it
                    if (phonesCursor != null) {
                        phonesCursor.close();
                    }

                    return numbers;
                }

                try {
                    if (phonesCursor != null && phonesCursor.moveToFirst()) {
                        names += ", " + phonesCursor.getString(0);
                    } else {
                        try {
                            names += ", " + PhoneNumberUtil.format(number[i]);
                        } catch (Exception e) {
                            names += ", " + number;
                        }
                    }
                } finally {
                    if (phonesCursor != null) {
                        phonesCursor.close();
                    }
                }
            } catch (IllegalArgumentException e) {
                try {
                    names += ", " + PhoneNumberUtil.format(number[i]);
                } catch (Exception f) {
                    names += ", " + number;
                }
            }
        }

        try {
            return names.substring(2);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Gets a contact image for a given phone number.
     *
     * @param number the phone number to find a contact for.
     * @param context the current application context.
     * @return the image uri or null if one could not be found.
     */
    public static String findImageUri(String number, Context context) {
        if (number.split(", ").length > 1) {
            return null;
        } else {
            Uri phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            ContentResolver cr = context.getContentResolver();

            try {
                Cursor contact = cr.query(phoneUri,
                        new String[]{ContactsContract.Contacts._ID}, null, null, null);

                if (contact.moveToFirst()) {
                    long userId = contact.getLong(contact.getColumnIndex(ContactsContract.Contacts._ID));
                    Uri photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, userId);
                    contact.close();

                    return photoUri.toString();
                } else {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }
    }

}
