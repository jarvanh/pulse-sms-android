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
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.Settings;

/**
 * Helper for working with timestamps on messages.
 */
public class TimeUtils {

    private static final long SECOND = 1000;
    private static final long MINUTE = SECOND * 60;
    private static final long HOUR = MINUTE * 60;
    private static final long DAY = HOUR * 24;
    private static final long YEAR = DAY * 365;

    /**
     * If the next timestamp is more than 15 minutes away, we will display it on the message.
     *
     * @param timestamp     the current message's timestamp.
     * @param nextTimestamp the next message's timestamp. This should be larger than timestamp.
     * @return true if we should display the timestamp, false otherwise.
     */
    public static boolean shouldDisplayTimestamp(long timestamp, long nextTimestamp) {
        return nextTimestamp >= timestamp + (15 * MINUTE);
    }

    /**
     * Checks whether the timestamp is on the same calendar day as today.
     *
     * @param timestamp the timestamp to check.
     * @return true if same calendar day, false otherwise.
     */
    public static boolean isToday(long timestamp) {
        return isToday(timestamp, System.currentTimeMillis());
    }

    @VisibleForTesting
    static boolean isToday(long timestamp, long currentTime) {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(currentTime);
        zeroCalendarDay(current);

        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timestamp);
        zeroCalendarDay(time);

        return current.getTimeInMillis() == time.getTimeInMillis();
    }

    /**
     * Checks whether the timestamp is on the same calendar day as yesterday.
     *
     * @param timestamp the timestamp to check.
     * @return if if yesterday, false otherwise.
     */
    public static boolean isYesterday(long timestamp) {
        return isYesterday(timestamp, System.currentTimeMillis());
    }

    @VisibleForTesting
    static boolean isYesterday(long timestamp, long currentTime) {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(currentTime);
        zeroCalendarDay(current);
        current.set(Calendar.DAY_OF_YEAR, current.get(Calendar.DAY_OF_YEAR) - 1);

        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timestamp);
        zeroCalendarDay(time);

        return current.getTimeInMillis() == time.getTimeInMillis();
    }

    /**
     * Checks whether the timestamp is within the last week.
     */
    public static boolean isLastWeek(long timestamp) {
        return isLastWeek(timestamp, System.currentTimeMillis());
    }

    @VisibleForTesting
    static boolean isLastWeek(long timestamp, long currentTime) {
        Calendar lastWeek = Calendar.getInstance();
        lastWeek.setTimeInMillis(currentTime);
        zeroCalendarDay(lastWeek);
        lastWeek.set(Calendar.WEEK_OF_YEAR, lastWeek.get(Calendar.WEEK_OF_YEAR) - 1);

        return timestamp > lastWeek.getTimeInMillis() && timestamp < currentTime;
    }

    /**
     * Checks whether the timestamp is within the last month.
     */
    public static boolean isLastMonth(long timestamp) {
        return isLastMonth(timestamp, System.currentTimeMillis());
    }

    @VisibleForTesting
    static boolean isLastMonth(long timestamp, long currentTime) {
        Calendar lastMonth = Calendar.getInstance();
        lastMonth.setTimeInMillis(currentTime);
        zeroCalendarDay(lastMonth);
        lastMonth.set(Calendar.MONTH, lastMonth.get(Calendar.MONTH) - 1);

        return timestamp > lastMonth.getTimeInMillis() && timestamp < currentTime;
    }

    private static void zeroCalendarDay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Formats the timestamp in a different way depending upon how long ago it was. Times within
     * 1 day will be just the timestamp (eg 7:30 PM). Times within 7 days will be the day and
     * the timestamp (eg Sun, 8:22 AM). Times older than that will be the date and the time
     * (eg 7/4/2016 12:25 PM). These will be formatted according to the device's default locale.
     *
     * @param timestamp the timestamp to format.
     * @return the formatted string.
     */
    public static String formatTimestamp(Context context, long timestamp) {
        return formatTimestamp(context, timestamp, System.currentTimeMillis());
    }

    @VisibleForTesting
    static String formatTimestamp(Context context, long timestamp, long currentTime) {
        Date date = new Date(timestamp);
        String formatted;

        if (timestamp > currentTime - (2 * MINUTE)) {
            formatted = context.getString(R.string.now);
        } else if (timestamp > currentTime - DAY) {
            formatted = DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
        } else if (timestamp > currentTime - (7 * DAY)) {
            formatted = new SimpleDateFormat("E", Locale.getDefault()).format(date) + ", " +
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
        } else if (timestamp > currentTime - YEAR) {
            formatted = new SimpleDateFormat("MMM d", Locale.getDefault()).format(date) + ", " +
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
        } else {
            formatted = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date) + ", " +
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
        }

        return formatted;
    }

    /**
     * Gets whether or not we are currently in the night time. This is defined as before 6 AM or
     * after 10 PM.
     */
    public static boolean isNight() {
        return isNight(Calendar.getInstance());
    }

    @VisibleForTesting
    static boolean isNight(Calendar cal) {
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour <= 6 || hour >= 20;
    }

    public static void setupNightTheme(AppCompatActivity activity) {
        Settings.BaseTheme base = Settings.get(activity).baseTheme;

        if (!base.isDark) {
            boolean isNight = TimeUtils.isNight() && base != Settings.BaseTheme.ALWAYS_LIGHT;
            activity.getDelegate().setLocalNightMode(isNight ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            AppCompatDelegate.setDefaultNightMode(isNight ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

}
