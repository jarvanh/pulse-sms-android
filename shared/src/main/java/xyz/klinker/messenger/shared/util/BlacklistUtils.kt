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

import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Blacklist

/**
 * Helper for checking whether or not a contact is blacklisted.
 */
object BlacklistUtils {

    fun isBlacklisted(context: Context, number: String): Boolean {
        val source = DataSource
        val cursor = source.getBlacklists(context)

        if (cursor.moveToFirst()) {
            val numberIndex = cursor.getColumnIndex(Blacklist.COLUMN_PHONE_NUMBER)

            do {
                val blacklisted = cursor.getString(numberIndex)
//                if (PhoneNumberUtils.checkEquality(number, blacklisted)) {
                if (numbersMatch(number, blacklisted)) {
                    CursorUtil.closeSilent(cursor)
                    return true
                }
            } while (cursor.moveToNext())
        }

        cursor.closeSilent()
        return false
    }

    fun numbersMatch(number: String, blacklisted: String): Boolean {
        val number = PhoneNumberUtils.clearFormatting(number).standardReplacements()
        val blacklisted = PhoneNumberUtils.clearFormatting(blacklisted).standardReplacements()

        val numberMatchers = SmsMmsUtils.createIdMatcher(number)
        val blacklistMatchers = SmsMmsUtils.createIdMatcher(blacklisted)

        val shorterLength = if (number.length < blacklisted.length) number.length else blacklisted.length

        return when {
            shorterLength >= 10 -> numberMatchers.tenLetter == blacklistMatchers.tenLetter
            shorterLength in 8..9 -> numberMatchers.eightLetter == blacklistMatchers.eightLetter
            shorterLength == 7 -> numberMatchers.sevenLetter == blacklistMatchers.sevenLetter
            number.length == blacklisted.length -> numberMatchers.fiveLetter == blacklistMatchers.fiveLetter
            else -> false
        }
    }

    private fun String.standardReplacements(): String {
        return this.replace("-".toRegex(), "")
                .replace(" ".toRegex(), "")
                .replace("/+".toRegex(), "")
                .replace("(", "")
                .replace(")", "")
    }
}
