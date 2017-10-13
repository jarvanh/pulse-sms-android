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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import xyz.klinker.messenger.shared.data.ColorSet;
import xyz.klinker.messenger.shared.data.DataSource;
import xyz.klinker.messenger.shared.data.FeatureFlags;
import xyz.klinker.messenger.shared.data.model.Contact;
import xyz.klinker.messenger.shared.data.model.Conversation;

/**
 * Helper for working with Android's contact provider.
 */
public class ContactUtils {

    // For the first lookup, we are matching on the content providers lookup method. This seems to match
    // the entire phone number.
    // The IMPROVE_CONTACT_MATCH feature flag will make it so, if that lookup fails, it will try to match part of the number, like
    // is done in the contact chips library. The issue with this is that shorter numbers will match and create an
    // invalid contact. For example, 456 for a bank number or something, would match contact number 5154569911
    // since 456 is in the contacts number. This isn't really what we want.
    // This field represents the minimum number of digits that the number needs to be, before it does the
    // advanced number lookup. That feature flag must also be on.
    private static final int MATCH_NUMBERS_WITH_SIZE_GREATER_THAN = 7;

    /**
     * Gets a space separated list of phone numbers.
     *
     * @param recipientIds the internal sms database recipient ids.
     * @param context      the application context.
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
                                        "_id=?", new String[]{ids[i]}, null);

                        if (number != null && number.moveToFirst()) {
                            String address = number.getString(number.getColumnIndex("address"));
                            String n = PhoneNumberUtils.clearFormatting(address);
                            if (n != null && n.length() > 0) {
                                numbers.add(n);
                            } else {
                                numbers.add(address);
                            }
                        } else {
                            numbers.add(ids[i]);
                        }

                        try {
                            number.close();
                        } catch (Exception e) { }
                    }
                } catch (Exception e) {
                    numbers.add("0");
                }
            }

            StringBuilder number = new StringBuilder();
            for (String n : numbers) {
                number.append(", ");
                number.append(n);
            }

            return number.toString().substring(2).replaceAll(",", ", ").replaceAll("  ", " ");
        } catch (Exception e) {
            return recipientIds;
        }
    }

    public static List<Conversation> queryContactGroups(Context context) {
        Cursor cursor = context.getContentResolver().query(ContactsContract.Groups.CONTENT_URI,
                new String[] { ContactsContract.Groups._ID, ContactsContract.Groups.TITLE },
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            final List<Conversation> conversations = new ArrayList<>();

            do {
                Conversation conversation = new Conversation();
                conversation.fillFromContactGroupCursor(context, cursor);
                conversations.add(conversation);
            } while (cursor.moveToNext());

            CursorUtil.closeSilent(cursor);

            for (int i = 0; i < conversations.size(); i++) {
                cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID},
                        ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "=?",
                        new String[] { conversations.get(i).getId() + "" }, null);

                if (cursor != null && cursor.moveToFirst() && cursor.getCount() < 150) {
                    String phoneNumbers = "";

                    do {
                        String num = ContactUtils.findPhoneNumberByContactId(context,
                                cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID)));
                        if (num != null) phoneNumbers += num + ", ";
                    } while (cursor.moveToNext());

                    if (phoneNumbers.length() > 0) {
                        conversations.get(i).setPhoneNumbers(phoneNumbers.substring(0, phoneNumbers.length() - 2));
                    } else {
                        conversations.remove(i);
                        i--;
                    }
                } else {
                    conversations.remove(i);
                    i--;
                }

                CursorUtil.closeSilent(cursor);
            }

            return conversations;
        }

        CursorUtil.closeSilent(cursor);
        return new ArrayList<>();
    }

    public static String findPhoneNumberByContactId(Context context, String rawContactId) {
        Cursor phoneNumber = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ ContactsContract.CommonDataKinds.Phone.NUMBER },
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                new String[] { rawContactId },null);

        String number = null;
        if (phoneNumber != null && phoneNumber.moveToFirst()) {
            number = phoneNumber.getString(0);
        }

        CursorUtil.closeSilent(phoneNumber);
        return number;
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
            if (numbers == null) {
                return "";
            } else {
                return numbers;
            }
        }

        for (int i = 0; i < number.length; i++) {
            String origin = number[i];

            try {
                Uri phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(origin));

                Cursor phonesCursor = context.getContentResolver()
                        .query(phoneUri, new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME },
                                null, null, null);

                if (phonesCursor != null && phonesCursor.moveToFirst()) {
                    names += ", " + phonesCursor.getString(0).replaceAll(",", "");
                } else if (origin.length() > MATCH_NUMBERS_WITH_SIZE_GREATER_THAN) {
                    phoneUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                            Uri.encode(origin));

                    phonesCursor = context.getContentResolver()
                            .query(phoneUri, new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME },
                                    null, null, null);

                    if (phonesCursor != null && phonesCursor.moveToFirst()) {
                        names += ", " + phonesCursor.getString(0).replaceAll(",", "");
                    } else {
                        try {
                            names += ", " + PhoneNumberUtils.format(number[i]);
                        } catch (Exception e) {
                            names += ", " + number;
                        }
                    }
                } else {
                    try {
                        names += ", " + PhoneNumberUtils.format(number[i]);
                    } catch (Exception e) {
                        names += ", " + number;
                    }
                }

                if (phonesCursor != null) {
                    phonesCursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            return names.substring(2);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Gets an id for the contact so that you can view that contact directly in the contacts app.
     */
    public static int findContactId(String number, Context context)
            throws NoSuchElementException {

        try {
            Uri phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number));

            Cursor phonesCursor = context.getContentResolver()
                            .query(phoneUri, new String[]{ContactsContract.PhoneLookup._ID},
                                    null, null, null);

            if (phonesCursor != null && phonesCursor.moveToFirst()) {
                int id = phonesCursor.getInt(0);
                phonesCursor.close();
                return id;
            } else if (number.length() > MATCH_NUMBERS_WITH_SIZE_GREATER_THAN) {
                phoneUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                        Uri.encode(number));

                phonesCursor = context.getContentResolver()
                        .query(phoneUri, new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID},
                                null, null, null);

                if (phonesCursor != null && phonesCursor.moveToFirst()) {
                    int id = phonesCursor.getInt(0);
                    phonesCursor.close();
                    return id;
                } else if (phonesCursor != null) {
                    phonesCursor.close();
                }
            } else if (phonesCursor != null) {
                phonesCursor.close();
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        throw new NoSuchElementException("Contact not found");
    }

    /**
     * Gets a contact image for a given phone number.
     *
     * @param number  the phone number to find a contact for.
     * @param context the current application context.
     * @return the image uri or null if one could not be found.
     */
    public static String findImageUri(String number, Context context) {
        String uri = null;

        if (number == null || number.split(", ").length > 1) {
            return null;
        } else {
            try {
                Uri phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(number));

                Cursor phonesCursor = context.getContentResolver()
                        .query(phoneUri, new String[]{ContactsContract.Contacts.PHOTO_THUMBNAIL_URI},
                                null, null, null);

                if (phonesCursor != null && phonesCursor.moveToFirst()) {
                    uri = phonesCursor.getString(0);
                    if (uri != null) {
                        uri = uri.replace("/photo", "");
                    }
                } else if (number.length() > MATCH_NUMBERS_WITH_SIZE_GREATER_THAN) {
                    phoneUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                            Uri.encode(number));

                    phonesCursor = context.getContentResolver()
                            .query(phoneUri, new String[]{ContactsContract.Contacts.PHOTO_THUMBNAIL_URI},
                                    null, null, null);

                    if (phonesCursor != null && phonesCursor.moveToFirst()) {
                        uri = phonesCursor.getString(0);
                        if (uri != null) {
                            uri = uri.replace("/photo", "");
                        }
                    }
                }

                if (phonesCursor != null) {
                    phonesCursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return uri;
        }
    }

    public static boolean shouldDisplayContactLetter(Conversation conversation) {
        if (conversation.getTitle().length() == 0 || conversation.getTitle().contains(", ") ||
                conversation.getPhoneNumbers().contains(", ")) {
            return false;
        } else {
            String firstLetter = conversation.getTitle().substring(0, 1);

            // if the first letter is a character and not a number or + or something weird, show it.
            if (Pattern.compile("[\\p{L}]").matcher(firstLetter).find()) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Get a list of contact objects from Android's database.
     */
    public static List<Contact> queryContacts(Context context, DataSource dataSource) {
        List<Contact> contacts = new ArrayList<>();
        List<Conversation> conversations = dataSource.getAllConversationsAsList(context);

        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        Cursor cursor = context.getContentResolver().query(
                uri,
                projection,
                ContactsContract.CommonDataKinds.Phone.TYPE + "=?",
                new String[] { Integer.toString(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE) },
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Contact contact = new Contact();

                contact.setName(cursor.getString(0));
                contact.setPhoneNumber(cursor.getString(1));

                ColorSet colorSet = getColorsFromConversation(conversations, contact.getName());
                if (colorSet != null) {
                    contact.setColors(colorSet);
                } else {
                    ImageUtils.fillContactColors(contact, ContactUtils.findImageUri(contact.getPhoneNumber(), context), context);
                }

                contacts.add(contact);
            } while (cursor.moveToNext());
        }

        CursorUtil.closeSilent(cursor);

        conversations.clear();
        return contacts;
    }

    /**
     * Get a list of contact objects from Android's database.
     */
    public static List<Contact> queryNewContacts(Context context, DataSource dataSource, long since) {
        List<Contact> contacts = new ArrayList<>();
        List<Conversation> conversations = new ArrayList<>();
        Cursor convoCursor = dataSource.getAllConversations(context);

        if (convoCursor.moveToFirst()) {
            do {
                Conversation conversation = new Conversation();
                conversation.fillFromCursor(convoCursor);
                conversations.add(conversation);
            } while (convoCursor.moveToNext());
        }

        try {
            convoCursor.close();
        } catch (Exception e) { }

        try {

            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String[] projection = new String[]{
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP
            };

            Cursor cursor = context.getContentResolver().query(
                    uri,
                    projection,
                    ContactsContract.CommonDataKinds.Phone.TYPE + "=? AND " +
                            ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP + " >= ?",
                    new String[]{Integer.toString(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE), Long.toString(since)},
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Contact contact = new Contact();

                    contact.setName(cursor.getString(0));
                    contact.setPhoneNumber(PhoneNumberUtils.clearFormatting(PhoneNumberUtils.format(cursor.getString(1))));

                    ColorSet colorSet = getColorsFromConversation(conversations, contact.getName());
                    if (colorSet != null) {
                        contact.setColors(colorSet);
                    } else {
                        ImageUtils.fillContactColors(contact, ContactUtils.findImageUri(contact.getPhoneNumber(), context), context);
                    }

                    contacts.add(contact);
                } while (cursor.moveToNext());

                cursor.close();
            }
        } catch (Throwable e) {
            // need permission, but don't have it in the background?
            // why wouldn't they grant permission to an sms app?
        }

        conversations.clear();
        return contacts;
    }

    /**
     * Get a list of colors for a contact, if an individual conversation exists for them.
     *
     * @param conversations all the conversations in the database
     * @return color set from the conversation if one exists, or null if one does not exist.
     */
    private static ColorSet getColorsFromConversation(List<Conversation> conversations, String name) {
        for (Conversation conversation : conversations) {
            if (conversation.getTitle().equals(name)) {
                return conversation.getColors();
            }
        }

        return null;
    }

    /**
     * Convert a list of contacts to a map with the message.message_from name as the key.
     *
     * If a number does not exist in the list, then it is given a new color scheme, added to the
     * database, and sends it off to the backend for storage. This way the color scheme can be saved
     * and used on other devices too.
     *
     * @param numbers the phone numbers from the conversation. Comma seperated list if there is more than one person.
     * @param contacts List of contacts from the database that should correspond to the people in the conversation
     */
    public static Map<String, Contact> getMessageFromMapping(String numbers, List<Contact> contacts,
                                                             DataSource dataSource, Context context) {
        Map<String, Contact> contactMap = new HashMap<>();
        String[] phoneNumbers = numbers.split(", ");

        for (String number : phoneNumbers) {
            Contact contact = getContactFromList(contacts, number);

            if (contact == null) {
                contact = new Contact();
                contact.setName(number);
                contact.setPhoneNumber(number);
                contact.setColors(ColorUtils.INSTANCE.getRandomMaterialColor(context));
                dataSource.insertContact(context, contact);
            }

            contactMap.put(contact.getName(), contact);
        }

        return contactMap;
    }

    /**
     * Convert a list of contacts to a map with the message.message_from name as the key.
     *
     * If a number does not exist in the list, then it is given a new color scheme, added to the
     * database, and sends it off to the backend for storage. This way the color scheme can be saved
     * and used on other devices too.
     *
     * @param convoTitle the title from the conversation. Comma seperated list if there is more than one person.
     * @param contacts List of contacts from the database that should correspond to the people in the conversation
     */
    public static Map<String, Contact> getMessageFromMappingByTitle(String convoTitle, List<Contact> contacts) {
        Map<String, Contact> contactMap = new HashMap<>();
        String[] names = convoTitle.split(", ");

        for (String name : names) {
            Contact contact = getContactFromListByName(contacts, name);
            contactMap.put(contact.getName(), contact);
        }

        return contactMap;
    }

    private static Contact getContactFromListByName(List<Contact> list, String name) {
        for (Contact contact : list) {
            if (contact.getName().equals(name)) {
                return contact;
            }
        }

        return null;
    }

    private static Contact getContactFromList(List<Contact> list, String number) {
        for (Contact contact : list) {
            if (PhoneNumberUtils.checkEquality(contact.getPhoneNumber(), number)) {
                return contact;
            }
        }

        return null;
    }

    /**
     * Remove the country code and just grab the last 10 characters, since that is probably
     * all we need to match in the database contact
     */
    public static String getPlainNumber(String number) {
        if (number == null) {
            return number;
        }

        number = number.replace("+", "");

        if (number.length() > 10) {
            return number.substring(number.length() - 10, number.length());
        } else {
            return number;
        }
    }
}
