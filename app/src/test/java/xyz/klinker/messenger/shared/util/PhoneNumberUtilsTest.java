/*
 * Copyright (C) 2020 Luke Klinker
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

import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PhoneNumberUtilsTest extends MessengerRobolectricSuite {

    @Test
    public void clearFormattingNone() {
        assertEquals("5154224558", PhoneNumberUtils.INSTANCE.clearFormatting("5154224558"));
    }

    @Test
    public void clearFormattingLeavePlus() {
        assertEquals("+15154224558", PhoneNumberUtils.INSTANCE.clearFormatting("+15154224558"));
    }

    @Test
    public void clearFormattingLeavePlusAndRemovesSpaces() {
        assertEquals("+33619943924", PhoneNumberUtils.INSTANCE.clearFormatting("+33 6 19 94 39 24"));
    }

    @Test
    public void clearFormatting() {
        assertEquals("5154224558", PhoneNumberUtils.INSTANCE.clearFormatting("(515) 422-4558"));
    }

    @Test
    public void clearFormattingSpaces() {
        assertEquals("15556667777", PhoneNumberUtils.INSTANCE.clearFormatting("1 555 666 7777"));
    }

    @Test
    public void clearFormattingMultipleNumbers() {
        assertEquals("5154224558,5159911493",
                PhoneNumberUtils.INSTANCE.clearFormatting("(515) 422-4558, (515) 991-1493"));
    }

    @Test
    public void clearFormattingEmail() {
        assertEquals("jklinker1@gmail.com", PhoneNumberUtils.INSTANCE.clearFormatting("jklinker1@gmail.com"));
    }

    @Test
    public void clearFormattingHiddenNumber() {
        assertEquals("Yahoo", PhoneNumberUtils.INSTANCE.clearFormatting("Yahoo"));
    }

    @Test
    public void format() {
        assertEquals("+1 515-422-4558", PhoneNumberUtils.INSTANCE.format("+15154224558"));
    }

    @Test
    public void formatShorter() {
        assertEquals("(515) 422-4558", PhoneNumberUtils.INSTANCE.format("5154224558"));
    }

    @Test
    public void formatShort() {
        assertEquals("22000", PhoneNumberUtils.INSTANCE.format("22000"));
    }

    @Test
    public void equals() {
        assertTrue(PhoneNumberUtils.INSTANCE.checkEquality("5154224558", "5154224558"));
        assertTrue(PhoneNumberUtils.INSTANCE.checkEquality("5154224558", "+15154224558"));
        assertTrue(PhoneNumberUtils.INSTANCE.checkEquality("+1 (515) 422-4558", "5154224558"));
        assertTrue(PhoneNumberUtils.INSTANCE.checkEquality("Yahoo", "Yahoo"));
    }

    @Test
    public void notEquals() {
        assertFalse(PhoneNumberUtils.INSTANCE.checkEquality("5154224558", "5159911493"));
        assertFalse(PhoneNumberUtils.INSTANCE.checkEquality("+15154224558", "+15159911493"));
        assertFalse(PhoneNumberUtils.INSTANCE.checkEquality("+15154224558", "+15254224558"));
        assertFalse(PhoneNumberUtils.INSTANCE.checkEquality("Jacob Klinker", "+15154224558"));
        assertFalse(PhoneNumberUtils.INSTANCE.checkEquality("", "GLOBE"));
    }

    @Test
    public void notEqualsEmailAddress() {
        assertFalse(PhoneNumberUtils.INSTANCE.checkEquality("jklinker1@gmail.com", "+15673935130"));
    }
    
    @Test
    public void displaysIncorrectBehavior() {
        // this actually shows the incorrect-ness of this method. These are clearly NOT the same:
        // https://github.com/klinker-apps/messenger-issues/issues/447
        assertTrue(PhoneNumberUtils.INSTANCE.checkEquality("VM-LIFSTL", "VM-HDFCBK"));
        assertTrue(PhoneNumberUtils.INSTANCE.checkEquality("VK-LIFSTL", "VK-HDFCBK"));
    }
}