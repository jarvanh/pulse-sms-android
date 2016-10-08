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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import xyz.klinker.messenger.MessengerSuite;
import xyz.klinker.messenger.R;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class TimeUtilsTest extends MessengerSuite {

    private static final long SECOND = 1000;
    private static final long MINUTE = SECOND * 60;
    private static final long HOUR = MINUTE * 60;
    private static final long DAY = HOUR * 24;

    private long current = System.currentTimeMillis();

    @Mock
    private Context context;

    @Before
    public void setUp() {
        when(context.getString(R.string.now)).thenReturn("Now");
    }

    @Test
    public void shouldNotDisplayTimestamp() {
        assertFalse(TimeUtils.shouldDisplayTimestamp(current, current + (5 * MINUTE)));
        assertFalse(TimeUtils.shouldDisplayTimestamp(current, current + SECOND));
        assertFalse(TimeUtils.shouldDisplayTimestamp(current, current + (14 * MINUTE) + (59 * SECOND)));
    }

    @Test
    public void shouldDisplayTimestamp() {
        assertTrue(TimeUtils.shouldDisplayTimestamp(current, current + (15 * MINUTE)));
        assertTrue(TimeUtils.shouldDisplayTimestamp(current, current + HOUR));
        assertTrue(TimeUtils.shouldDisplayTimestamp(current, current + (2 * DAY)));
    }

    @Test
    public void displayTimeAsNow() {
        assertEquals("Now", TimeUtils.formatTimestamp(context, current));
    }

    @Test
    public void displayTimeAsShort() {
        long time = current - (5 * HOUR);
        assertEquals(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(time)),
                TimeUtils.formatTimestamp(context, time));
    }

    @Test
    public void displayTimeAsMedium() {
        long time = current - (2 * DAY);
        String expected = new SimpleDateFormat("E").format(new Date(time)) + ", " +
                DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(time));
        assertEquals(expected, TimeUtils.formatTimestamp(context, time));
    }

    @Test
    public void displayTimeAsLong() {
        long time = current - (8 * DAY);
        String expected = new SimpleDateFormat("MMM d").format(new Date(time)) + ", " +
                DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(time));
        assertEquals(expected, TimeUtils.formatTimestamp(context, time));
    }

    @Test
    public void displayTimeAsExtraLong() {
        long time = current - (400 * DAY);
        String expected = new SimpleDateFormat("MMM d, yyyy").format(new Date(time)) + ", " +
                DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(time));
        assertEquals(expected, TimeUtils.formatTimestamp(context, time));
    }

    @Test
    public void displayTimeAsShortActual() {
        long currentTime = new GregorianCalendar(2016, 6, 13, 8, 23).getTimeInMillis();
        long timestamp = currentTime - (8 * HOUR);

        assertEquals("12:23 AM", TimeUtils.formatTimestamp(context, timestamp, currentTime));
    }

    @Test
    public void displayTimeAsMediumActual() {
        long currentTime = new GregorianCalendar(2016, 6, 13, 8, 23).getTimeInMillis();
        long timestamp = currentTime - (8 * HOUR) - (3 * DAY);

        assertEquals("Sun, 12:23 AM", TimeUtils.formatTimestamp(context, timestamp, currentTime));
    }

    @Test
    public void displayTimeAsLongActual() {
        long currentTime = new GregorianCalendar(2016, 6, 13, 8, 23).getTimeInMillis();
        long timestamp = currentTime - (8 * HOUR) - (8 * DAY);

        assertEquals("Jul 5, 12:23 AM", TimeUtils.formatTimestamp(context, timestamp, currentTime));
    }

    @Test
    public void displayTimeAsExtraLongActual() {
        long currentTime = new GregorianCalendar(2016, 6, 13, 8, 23).getTimeInMillis();
        long timestamp = currentTime - (400 * DAY);

        assertEquals("Jun 9, 2015, 8:23 AM", TimeUtils.formatTimestamp(context, timestamp, currentTime));
    }

    @Test
    public void isToday() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertTrue(TimeUtils.isToday(
                new GregorianCalendar(2016, 7, 12, 12, 22, 0).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtils.isToday(
                new GregorianCalendar(2016, 7, 12, 0, 4, 56).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtils.isToday(
                new GregorianCalendar(2016, 7, 12, 17, 52, 16).getTimeInMillis(),
                currentTime
        ));
    }

    @Test
    public void isNotToday() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertFalse(TimeUtils.isToday(
                new GregorianCalendar(2016, 7, 11, 23, 12, 5).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtils.isToday(
                new GregorianCalendar(2016, 7, 11, 7, 56, 0).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtils.isToday(
                new GregorianCalendar(2016, 7, 13, 15, 45, 26).getTimeInMillis(),
                currentTime
        ));
    }

    @Test
    public void isYesterday() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertTrue(TimeUtils.isYesterday(
                new GregorianCalendar(2016, 7, 11, 12, 22, 0).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtils.isYesterday(
                new GregorianCalendar(2016, 7, 11, 0, 4, 56).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtils.isYesterday(
                new GregorianCalendar(2016, 7, 11, 17, 52, 16).getTimeInMillis(),
                currentTime
        ));
    }

    @Test
    public void isNotYesterday() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertFalse(TimeUtils.isYesterday(
                new GregorianCalendar(2016, 7, 12, 23, 12, 5).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtils.isYesterday(
                new GregorianCalendar(2016, 7, 10, 7, 56, 0).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtils.isYesterday(
                new GregorianCalendar(2016, 7, 13, 15, 45, 26).getTimeInMillis(),
                currentTime
        ));
    }

    @Test
    public void isLastWeek() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertTrue(TimeUtils.isLastWeek(
                new GregorianCalendar(2016, 7, 10, 12, 22, 0).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtils.isLastWeek(
                new GregorianCalendar(2016, 7, 7, 0, 4, 56).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtils.isLastWeek(
                new GregorianCalendar(2016, 7, 6, 17, 52, 16).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtils.isLastWeek(
                new GregorianCalendar(2016, 7, 12, 11, 52, 16).getTimeInMillis(),
                currentTime
        ));
    }

    @Test
    public void isNotLastWeek() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertFalse(TimeUtils.isLastWeek(
                new GregorianCalendar(2016, 7, 12, 23, 12, 5).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtils.isLastWeek(
                new GregorianCalendar(2016, 7, 4, 7, 56, 0).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtils.isLastWeek(
                new GregorianCalendar(2016, 5, 13, 15, 45, 26).getTimeInMillis(),
                currentTime
        ));
    }

    @Test
    public void isLastMonth() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertTrue(TimeUtils.isLastMonth(
                new GregorianCalendar(2016, 7, 11, 12, 22, 0).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtils.isLastMonth(
                new GregorianCalendar(2016, 7, 1, 0, 4, 56).getTimeInMillis(),
                currentTime
        ));

        assertTrue(TimeUtils.isLastMonth(
                new GregorianCalendar(2016, 6, 13, 17, 52, 16).getTimeInMillis(),
                currentTime
        ));
    }

    @Test
    public void isNotLastMonth() {
        long currentTime = new GregorianCalendar(2016, 7, 12, 15, 45, 26).getTimeInMillis();

        assertFalse(TimeUtils.isLastMonth(
                new GregorianCalendar(2016, 7, 12, 23, 12, 5).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtils.isLastMonth(
                new GregorianCalendar(2015, 7, 10, 7, 56, 0).getTimeInMillis(),
                currentTime
        ));

        assertFalse(TimeUtils.isLastMonth(
                new GregorianCalendar(2016, 5, 13, 15, 45, 26).getTimeInMillis(),
                currentTime
        ));
    }

    @Test
    public void isNight() {
        assertTrue(TimeUtils.isNight(new GregorianCalendar(2016, 8, 16, 2, 30)));
        assertTrue(TimeUtils.isNight(new GregorianCalendar(2016, 8, 16, 5, 59)));
        assertTrue(TimeUtils.isNight(new GregorianCalendar(2016, 8, 16, 22, 0)));
        assertTrue(TimeUtils.isNight(new GregorianCalendar(2016, 8, 16, 23, 59)));
    }

    @Test
    public void isNotNight() {
        assertFalse(TimeUtils.isNight(new GregorianCalendar(2016, 8, 16, 6, 0)));
        assertFalse(TimeUtils.isNight(new GregorianCalendar(2016, 8, 16, 13, 30)));
        assertFalse(TimeUtils.isNight(new GregorianCalendar(2016, 8, 16, 15, 50)));
        assertFalse(TimeUtils.isNight(new GregorianCalendar(2016, 8, 16, 19, 59)));
    }

}