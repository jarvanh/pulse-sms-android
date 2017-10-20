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

package xyz.klinker.messenger.shared.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

import java.util.ArrayList
import java.util.HashMap
import java.util.NoSuchElementException
import java.util.regex.Pattern

import xyz.klinker.messenger.shared.data.ColorSet
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.model.Contact
import xyz.klinker.messenger.shared.data.model.Conversation

/**
 * Helper for working with Android's contact provider.
 */
object ContactUtils {

    // For the first lookup, we are matching on the content providers lookup method. This seems to match
    // the entire phone number.
    // The IMPROVE_CONTACT_MATCH feature flag will make it so, if that lookup fails, it will try to match part of the number, like
    // is done in the contact chips library. The issue with this is that shorter numbers will match and create an
    // invalid contact. For example, 456 for a bank number or something, would match contact number 5154569911
    // since 456 is in the contacts number. This isn't really what we want.
    // This field represents the minimum number of digits that the number needs to be, before it does the
    // advanced number lookup. That feature flag must also be on.
    private val MATCH_NUMBERS_WITH_SIZE_GREATER_THAN = 7

    /**
     * Gets a space separated list of phone numbers.
     *
     * @param recipientIds the internal sms database recipient ids.
     * @param context      the application context.
     * @return the comma and space separated list of numbers.
     */
    fun findContactNumbers(recipientIds: String, context: Context): String {
        try {
            val ids = recipientIds.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val numbers = ArrayList<String>()

            for (i in ids.indices) {
                try {
                    if (ids[i] != "" || ids[i] != " ") {
                        val number = context.contentResolver
                                .query(Uri.parse("content://mms-sms/canonical-addresses"), null,
                                        "_id=?", arrayOf(ids[i]), null)

                        if (number != null && number.moveToFirst()) {
                            val address = number.getString(number.getColumnIndex("address"))
                            val n = PhoneNumberUtils.clearFormatting(address)
                            if (n != null && n.isNotEmpty()) {
                                numbers.add(n)
                            } else {
                                numbers.add(address)
                            }
                        } else {
                            numbers.add(ids[i])
                        }

                        number?.closeSilent()

                    }
                } catch (e: Exception) {
                    numbers.add("0")
                }

            }

            val number = StringBuilder()
            for (n in numbers) {
                number.append(", ")
                number.append(n)
            }

            return number.toString().substring(2).replace(",".toRegex(), ", ").replace("  ".toRegex(), " ")
        } catch (e: Exception) {
            return recipientIds
        }

    }

    fun queryContactGroups(context: Context): List<Conversation> {
        var cursor = context.contentResolver.query(ContactsContract.Groups.CONTENT_URI,
                arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE), null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            val conversations = ArrayList<Conversation>()

            do {
                val conversation = Conversation()
                conversation.fillFromContactGroupCursor(context, cursor)
                conversations.add(conversation)
            } while (cursor.moveToNext())

            cursor.closeSilent()

            var i = 0
            while (i < conversations.size) {
                cursor = context.contentResolver.query(ContactsContract.Data.CONTENT_URI,
                        arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID),
                        ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "=?",
                        arrayOf(conversations[i].id.toString() + ""), null)

                if (cursor != null && cursor.moveToFirst() && cursor.count < 150) {
                    var phoneNumbers = ""

                    do {
                        val num = ContactUtils.findPhoneNumberByContactId(context,
                                cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID)))
                        if (num != null) phoneNumbers += num + ", "
                    } while (cursor.moveToNext())

                    if (phoneNumbers.isNotEmpty()) {
                        conversations[i].phoneNumbers = phoneNumbers.substring(0, phoneNumbers.length - 2)
                    } else {
                        conversations.removeAt(i)
                        i--
                    }
                } else {
                    conversations.removeAt(i)
                    i--
                }

                cursor.closeSilent()
                i++
            }

            return conversations
        }

        cursor.closeSilent()
        return ArrayList()
    }

    private fun findPhoneNumberByContactId(context: Context, rawContactId: String): String? {
        val phoneNumber = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?",
                arrayOf(rawContactId), null)

        var number: String? = null
        if (phoneNumber != null && phoneNumber.moveToFirst()) {
            number = phoneNumber.getString(0)
        }

        CursorUtil.closeSilent(phoneNumber)
        return number
    }

    /**
     * Gets a comma and space separated list of names from numbers.
     *
     * @param numbers the comma and space separated list of phone numbers to look up.
     * @param context the current application context.
     * @return a space separated list of names.
     */
    fun findContactNames(numbers: String?, context: Context?): String {
        if (context == null) {
            return ""
        }

        var names = ""
        val number: Array<String>

        try {
            number = numbers!!.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        } catch (e: Exception) {
            return numbers ?: ""
        }

        for (i in number.indices) {
            val origin = number[i]

            try {
                var phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(origin))

                var phonesCursor = context.contentResolver
                        .query(phoneUri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)

                if (phonesCursor != null && phonesCursor.moveToFirst()) {
                    names += ", " + phonesCursor.getString(0).replace(",".toRegex(), "")
                } else if (origin.length > MATCH_NUMBERS_WITH_SIZE_GREATER_THAN) {
                    phoneUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                            Uri.encode(origin))

                    phonesCursor = context.contentResolver
                            .query(phoneUri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)

                    names += if (phonesCursor != null && phonesCursor.moveToFirst()) {
                        ", " + phonesCursor.getString(0).replace(",".toRegex(), "")
                    } else {
                        try {
                            ", " + PhoneNumberUtils.format(number[i])!!
                        } catch (e: Exception) {
                            ", " + number
                        }
                    }
                } else {
                    names += try {
                        ", " + PhoneNumberUtils.format(number[i])!!
                    } catch (e: Exception) {
                        ", " + number
                    }

                }

                phonesCursor?.closeSilent()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        return try {
            names.substring(2)
        } catch (e: Exception) {
            ""
        }

    }

    /**
     * Gets an id for the contact so that you can view that contact directly in the contacts app.
     */
    @Throws(NoSuchElementException::class)
    fun findContactId(number: String, context: Context): Int {

        try {
            var phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number))

            var phonesCursor = context.contentResolver
                    .query(phoneUri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)

            if (phonesCursor != null && phonesCursor.moveToFirst()) {
                val id = phonesCursor.getInt(0)
                phonesCursor.close()
                return id
            } else if (number.length > MATCH_NUMBERS_WITH_SIZE_GREATER_THAN) {
                phoneUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                        Uri.encode(number))

                phonesCursor = context.contentResolver
                        .query(phoneUri, arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID), null, null, null)

                if (phonesCursor != null && phonesCursor.moveToFirst()) {
                    val id = phonesCursor.getInt(0)
                    phonesCursor.close()
                    return id
                } else if (phonesCursor != null) {
                    phonesCursor.close()
                }
            } else if (phonesCursor != null) {
                phonesCursor.close()
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }

        throw NoSuchElementException("Contact not found")
    }

    /**
     * Gets a contact image for a given phone number.
     *
     * @param number  the phone number to find a contact for.
     * @param context the current application context.
     * @return the image uri or null if one could not be found.
     */
    fun findImageUri(number: String?, context: Context?): String? {
        if (context == null) {
            return null
        }

        var uri: String? = null

        if (number == null || number.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size > 1) {
            return null
        } else {
            try {
                var phoneUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(number))

                var phonesCursor = context.contentResolver
                        .query(phoneUri, arrayOf(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI), null, null, null)

                if (phonesCursor != null && phonesCursor.moveToFirst()) {
                    uri = phonesCursor.getString(0)
                    if (uri != null) {
                        uri = uri.replace("/photo", "")
                    }
                } else if (number.length > MATCH_NUMBERS_WITH_SIZE_GREATER_THAN) {
                    phoneUri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
                            Uri.encode(number))

                    phonesCursor = context.contentResolver
                            .query(phoneUri, arrayOf(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI), null, null, null)

                    if (phonesCursor != null && phonesCursor.moveToFirst()) {
                        uri = phonesCursor.getString(0)
                        if (uri != null) {
                            uri = uri.replace("/photo", "")
                        }
                    }
                }

                phonesCursor?.closeSilent()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return uri
        }
    }

    fun shouldDisplayContactLetter(conversation: Conversation?) =
            if (conversation == null || conversation.title!!.isEmpty() || conversation.title!!.contains(", ") ||
                    conversation.phoneNumbers!!.contains(", ")) {
                false
            } else {
                val firstLetter = conversation.title!!.substring(0, 1)

                // if the first letter is a character and not a number or + or something weird, show it.
                Pattern.compile("[\\p{L}]").matcher(firstLetter).find()
            }

    /**
     * Get a list of contact objects from Android's database.
     */
    fun queryContacts(context: Context, dataSource: DataSource): List<Contact> {
        val contacts = ArrayList<Contact>()
        val conversations = dataSource.getAllConversationsAsList(context)

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)

        val cursor = context.contentResolver.query(
                uri,
                projection,
                ContactsContract.CommonDataKinds.Phone.TYPE + "=?",
                arrayOf(Integer.toString(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)), null
        )

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val contact = Contact()

                contact.name = cursor.getString(0)
                contact.phoneNumber = cursor.getString(1)

                val colorSet = getColorsFromConversation(conversations, contact.name)
                if (colorSet != null) {
                    contact.colors = colorSet
                } else {
                    ImageUtils.fillContactColors(contact, ContactUtils.findImageUri(contact.phoneNumber, context), context)
                }

                contacts.add(contact)
            } while (cursor.moveToNext())
        }

        cursor.closeSilent()
        return contacts
    }

    /**
     * Get a list of contact objects from Android's database.
     */
    fun queryNewContacts(context: Context, dataSource: DataSource, since: Long): List<Contact> {
        val contacts = ArrayList<Contact>()
        val conversations = ArrayList<Conversation>()
        val convoCursor = dataSource.getAllConversations(context)

        if (convoCursor.moveToFirst()) {
            do {
                val conversation = Conversation()
                conversation.fillFromCursor(convoCursor)
                conversations.add(conversation)
            } while (convoCursor.moveToNext())
        }

        convoCursor.closeSilent()

        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP)

            val cursor = context.contentResolver.query(
                    uri,
                    projection,
                    ContactsContract.CommonDataKinds.Phone.TYPE + "=? AND " +
                            ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP + " >= ?",
                    arrayOf(Integer.toString(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE), java.lang.Long.toString(since)), null
            )

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    val contact = Contact()

                    contact.name = cursor.getString(0)
                    contact.phoneNumber = PhoneNumberUtils.clearFormatting(PhoneNumberUtils.format(cursor.getString(1)))

                    val colorSet = getColorsFromConversation(conversations, contact.name)
                    if (colorSet != null) {
                        contact.colors = colorSet
                    } else {
                        ImageUtils.fillContactColors(contact, ContactUtils.findImageUri(contact.phoneNumber, context), context)
                    }

                    contacts.add(contact)
                } while (cursor.moveToNext())

                cursor.closeSilent()
            }
        } catch (e: Throwable) {
            // need permission, but don't have it in the background?
            // why wouldn't they grant permission to an sms app?
        }

        return contacts
    }

    /**
     * Get a list of colors for a contact, if an individual conversation exists for them.
     *
     * @param conversations all the conversations in the database
     * @return color set from the conversation if one exists, or null if one does not exist.
     */
    private fun getColorsFromConversation(conversations: List<Conversation>, name: String?): ColorSet? {
        return conversations
                .firstOrNull { it.title == name }
                ?.colors
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
    fun getMessageFromMapping(numbers: String, contacts: List<Contact>,
                              dataSource: DataSource, context: Context): Map<String, Contact> {
        val contactMap = HashMap<String, Contact>()
        val phoneNumbers = numbers.split(", ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        for (number in phoneNumbers) {
            var contact = getContactFromList(contacts, number)

            if (contact == null) {
                contact = Contact()
                contact.name = number
                contact.phoneNumber = number
                contact.colors = ColorUtils.getRandomMaterialColor(context)
                dataSource.insertContact(context, contact)
            }

            contactMap.put(contact.name!!, contact)
        }

        return contactMap
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
    fun getMessageFromMappingByTitle(convoTitle: String, contacts: List<Contact>): Map<String, Contact> {
        val contactMap = HashMap<String, Contact>()
        convoTitle.split(", ".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
                .map { getContactFromListByName(contacts, it) }
                .forEach { contactMap.put(it!!.name!!, it) }

        return contactMap
    }

    private fun getContactFromListByName(list: List<Contact>, name: String): Contact? {
        return list.firstOrNull { it.name == name }
    }

    private fun getContactFromList(list: List<Contact>, number: String): Contact? {
        return list.firstOrNull { PhoneNumberUtils.checkEquality(it.phoneNumber!!, number) }
    }

    /**
     * Remove the country code and just grab the last 10 characters, since that is probably
     * all we need to match in the database contact
     */
    fun getPlainNumber(number: String?): String? {
        var number = number
        if (number == null) {
            return number
        }

        number = number.replace("+", "")

        return if (number.length > 10) {
            number.substring(number.length - 10, number.length)
        } else {
            number
        }
    }
}
