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
import android.telephony.PhoneNumberUtils;

import com.klinker.android.send_message.Utils;

import java.util.Locale;

/**
 * Helper for working with phone numbers, mainly formatting them.
 */
public class PhoneNumberUtil {

    /**
     * Removes any formatted characters from a number, leaving just the number and maybe a + in the
     * front.
     *
     * @param number the number to clear formatting from.
     * @return the plain phone number.
     */
    public static String clearFormatting(String number) {
        return PhoneNumberUtils.stripSeparators(number);
    }

    /**
     * Formats a plain number into something more readable.
     *
     * @param number the number to format.
     * @return the formatted number.
     */
    public static String format(String number) {
        String formatted = PhoneNumberUtils.formatNumber(number, Locale.getDefault().getCountry());

        if (formatted == null) {
            return number;
        } else {
            return formatted;
        }
    }

    /**
     * Returns the device's phone number.
     */
    public static String getMyPhoneNumber(Context context) {
        return clearFormatting(Utils.getMyPhoneNumber(context));
    }

}
