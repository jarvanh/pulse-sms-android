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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import xyz.klinker.messenger.MessengerSuite;

import static org.junit.Assert.*;

public class TimeUtilTest extends MessengerSuite {

    private static final int SECOND = 1000;
    private static final int MINUTE = SECOND * 60;
    private static final int HOUR = MINUTE * 60;
    private static final int DAY = HOUR * 24;

    private long current = System.currentTimeMillis();

    @Test
    public void shouldNotDisplayTimestamp() {
        assertFalse(TimeUtil.shouldDisplayTimestamp(current, current + (5*MINUTE)));
        assertFalse(TimeUtil.shouldDisplayTimestamp(current, current + SECOND));
        assertFalse(TimeUtil.shouldDisplayTimestamp(current, current + (14*MINUTE) + (59*SECOND)));
    }

    @Test
    public void shouldDisplayTimestamp() {
        assertTrue(TimeUtil.shouldDisplayTimestamp(current, current + (15*MINUTE)));
        assertTrue(TimeUtil.shouldDisplayTimestamp(current, current + HOUR));
        assertTrue(TimeUtil.shouldDisplayTimestamp(current, current + (2*DAY)));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionWhenNextIsGreaterThanCurrent() {
        TimeUtil.shouldDisplayTimestamp(current, current - MINUTE);
    }

    @Test
    public void displayTimeAsShort() {
        assertEquals(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(current)),
                TimeUtil.formatTimestamp(current));
    }

    @Test
    public void displayTimeAsMedium() {
        long time = current - (2*DAY);
        String expected = new SimpleDateFormat("E").format(new Date(time)) + ", " +
                DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(time));
        assertEquals(expected, TimeUtil.formatTimestamp(time));
    }

    @Test
    public void displayTimeAsLong() {
        long time = current - (8*DAY);
        assertEquals(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(time)),
                TimeUtil.formatTimestamp(time));
    }

    @Test
    public void isToday() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertTrue(TimeUtil.isToday(
                new GregorianCalendar(2016, 7, 12, 12, 22, 0).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtil.isToday(
                new GregorianCalendar(2016, 7, 12, 0, 4, 56).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtil.isToday(
                new GregorianCalendar(2016, 7, 12, 17, 52, 16).getTimeInMillis(),
                currentTime
        ));
    }

    @Test
    public void isNotToday() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertFalse(TimeUtil.isToday(
                new GregorianCalendar(2016, 7, 11, 23, 12, 5).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtil.isToday(
                new GregorianCalendar(2016, 7, 11, 7, 56, 0).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtil.isToday(
                new GregorianCalendar(2016, 7, 13, 15, 45, 26).getTimeInMillis(),
                currentTime
        ));
    }

    @Test
    public void isYesterday() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertTrue(TimeUtil.isYesterday(
                new GregorianCalendar(2016, 7, 11, 12, 22, 0).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtil.isYesterday(
                new GregorianCalendar(2016, 7, 11, 0, 4, 56).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtil.isYesterday(
                new GregorianCalendar(2016, 7, 11, 17, 52, 16).getTimeInMillis(),
                currentTime
        ));
    }

    @Test
    public void isNotYesterday() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertFalse(TimeUtil.isYesterday(
                new GregorianCalendar(2016, 7, 12, 23, 12, 5).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtil.isYesterday(
                new GregorianCalendar(2016, 7, 10, 7, 56, 0).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtil.isYesterday(
                new GregorianCalendar(2016, 7, 13, 15, 45, 26).getTimeInMillis(),
                currentTime
        ));
    }

}