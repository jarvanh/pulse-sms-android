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

import static org.junit.Assert.assertEquals;

public class SmsMmsUtilsTest {

    @Test
    public void createIdMatcherSingleNumber() {
        assertEquals("11555", SmsMmsUtils.createIdMatcher("+15154211555"));
    }

    @Test
    public void createIdMatcherMultipleNumbers() {
        assertEquals("085321149396726",
                SmsMmsUtils.createIdMatcher("5154196726, 5154808532, 5159911493"));
    }

    @Test
    public void createIdMatcherEmail() {
        assertEquals("jklinker1@gmail.com",
                SmsMmsUtils.createIdMatcher("jklinker1@gmail.com"));
    }

    @Test
    public void createIdMatcherForSpacedWeirdNumbers() {
        assertEquals(SmsMmsUtils.createIdMatcher("987 654 3210"),
                SmsMmsUtils.createIdMatcher("1 987-654-3210"));
    }

}