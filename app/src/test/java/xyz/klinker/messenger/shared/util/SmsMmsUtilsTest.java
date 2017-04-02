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

package xyz.klinker.messenger.shared.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SmsMmsUtilsTest {

    @Test
    public void createIdMatcherSingleNumber() {
        assertEquals("4211555", SmsMmsUtils.createIdMatcher("+15154211555").sevenLetter);
    }

    @Test
    public void createIdMatcherMultipleNumbers() {
        assertEquals("419672648085329911493",
                SmsMmsUtils.createIdMatcher("5154196726, 5154808532, 5159911493").sevenLetter);
    }

    @Test
    public void createIdMatcherEmail() {
        assertEquals("jklinker1@gmail.com",
                SmsMmsUtils.createIdMatcher("jklinker1@gmail.com").sevenLetter);
    }

    @Test
    public void createIdMatcherForSpacedWeirdNumbers() {
        assertEquals(SmsMmsUtils.createIdMatcher("987 654 3210").sevenLetter,
                SmsMmsUtils.createIdMatcher("1 987-654-3210").sevenLetter);
    }

    @Test
    public void stripDuplicatePhoneNumbers_handlesEmpty() {
        assertEquals("", SmsMmsUtils.stripDuplicatePhoneNumbers(null));
        assertEquals("", SmsMmsUtils.stripDuplicatePhoneNumbers(""));
    }

    @Test
    public void stripDuplicatePhoneNumbers_handlesSingle() {
        assertEquals("5159911493", SmsMmsUtils.stripDuplicatePhoneNumbers("5159911493"));
    }

    @Test
    public void stripDuplicatePhoneNumbers_handlesMultiple() {
        assertEquals("661223, 5159911493", SmsMmsUtils.stripDuplicatePhoneNumbers("5159911493, 661223"));
    }

    @Test
    public void stripDuplicatePhoneNumbers_handlesDuplicates() {
        assertEquals("661223, 5159911493", SmsMmsUtils.stripDuplicatePhoneNumbers("5159911493, 661223, 661223"));
    }

}