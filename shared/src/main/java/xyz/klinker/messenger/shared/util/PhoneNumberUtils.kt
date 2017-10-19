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
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.TextUtils

import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Utils

import java.util.ArrayList
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

import xyz.klinker.messenger.api.implementation.Account

/**
 * Helper for working with phone numbers, mainly formatting them.
 */
object PhoneNumberUtils {

    private val EMAIL_ADDRESS_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
    )

    private val NAME_ADDR_EMAIL_PATTERN = Pattern.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*")

    /**
     * Removes any formatted characters from a number, leaving just the number and maybe a + in the
     * front.
     *
     * @param number the number to clear formatting from.
     * @return the plain phone number.
     */
    fun clearFormatting(number: String?): String {
        if (number == null) {
            return ""
        }

        return if (number.matches(".*[a-zA-Z].*".toRegex())) {
            number
        } else if (!isEmailAddress(number)) {
            android.telephony.PhoneNumberUtils.stripSeparators(number)
        } else {
            number
        }
    }

    private fun isEmailAddress(address: String): Boolean {
        if (TextUtils.isEmpty(address)) {
            return false
        }

        val s = extractAddrSpec(address)
        val match = EMAIL_ADDRESS_PATTERN.matcher(s)
        return match.matches()
    }

    private fun extractAddrSpec(address: String): String {
        val match = NAME_ADDR_EMAIL_PATTERN.matcher(address)

        return if (match.matches()) {
            match.group(2)
        } else address
    }


    /**
     * Formats a plain number into something more readable.
     *
     * @param number the number to format.
     * @return the formatted number.
     */
    fun format(number: String?): String? {
        if (number == null) {
            return null
        }

        val formatted = android.telephony.PhoneNumberUtils
                .formatNumber(number, Locale.getDefault().country)

        return formatted ?: number
    }

    /**
     * Returns the most likely device phone number
     */
    @JvmOverloads
    fun getMyPhoneNumber(context: Context, useSettings: Boolean = true): String {
        val numbers = getMyPossiblePhoneNumbers(context, useSettings)
        return if (numbers.isNotEmpty()) {
            numbers[0]
        } else {
            ""
        }
    }

    /**
     * Returns the devices possible phone numbers. (Account, Lollipop method, legacy method)
     */
    fun getMyPossiblePhoneNumbers(context: Context?, useSettings: Boolean = true): List<String> {
        val numbers = ArrayList<String>()

        if (useSettings) {
            val settings = xyz.klinker.messenger.shared.data.Settings
            if (settings.phoneNumber != null) {
                numbers.add(settings.phoneNumber!!)
            }
        }

        val account = Account
        if (account.exists() && account.myPhoneNumber != null && !account.myPhoneNumber!!.isEmpty()) {
            numbers.add(account.myPhoneNumber!!)
        }

        val lollipopNumber = getLollipopPhoneNumber(context)
        if (lollipopNumber != null && !lollipopNumber.isEmpty()) {
            numbers.add(lollipopNumber)
        }

        try {
            val legacyNumber = Utils.getMyPhoneNumber(context)
            if (legacyNumber != null && !legacyNumber.isEmpty()) {
                numbers.add(legacyNumber)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        return numbers
    }

    private fun getLollipopPhoneNumber(context: Context?): String? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val manager = SubscriptionManager.from(context)
                val availableSims = manager.activeSubscriptionInfoList

                if (availableSims != null && availableSims.size > 0) {
                    return availableSims[0].number
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * Parses a list of addresses coming from a URI string such as sms: or smsto:.
     */
    fun parseAddress(uriAddress: String): Array<String> {
        val number = clearFormatting(uriAddress) ?: return emptyArray()
        return number.replace("sms:", "")
                .replace("smsto:", "")
                .replace("mms:", "")
                .replace("mmsto:", "")
                .split(",".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
    }

    /**
     * Checks the equality of 2 phone numbers. We should account for sometimes the number being
     * the same even if one has a +1 in front of it or something like that.
     */
    fun checkEquality(number1: String, number2: String): Boolean {
        return android.telephony.PhoneNumberUtils.compare(number1, number2)
    }

}
