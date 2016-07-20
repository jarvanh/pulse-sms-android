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

import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;

import static org.junit.Assert.*;

public class PhoneNumberUtilTest extends MessengerRobolectricSuite {

    @Test
    public void clearFormattingNone() {
        assertEquals("5154224558", PhoneNumberUtil.clearFormatting("5154224558"));
    }

    @Test
    public void clearFormattingLeavePlus() {
        assertEquals("+15154224558", PhoneNumberUtil.clearFormatting("+15154224558"));
    }

    @Test
    public void clearFormatting() {
        assertEquals("5154224558", PhoneNumberUtil.clearFormatting("(515) 422-4558"));
    }

    @Test
    public void clearFormattingMultipleNumbers() {
        assertEquals("5154224558,5159911493",
                PhoneNumberUtil.clearFormatting("(515) 422-4558, (515) 991-1493"));
    }

    @Test
    public void format() {
        assertEquals("+1 515-422-4558", PhoneNumberUtil.format("+15154224558"));
    }

    @Test
    public void formatShorter() {
        assertEquals("(515) 422-4558", PhoneNumberUtil.format("5154224558"));
    }

    @Test
    public void formatShort() {
        assertEquals("22000", PhoneNumberUtil.format("22000"));
    }

}