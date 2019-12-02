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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.data.pojo.BaseTheme
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper for working with timestamps on messages.
 */
object TimeUtils {

    val now: Long
        get() = Date().time

    val SECOND: Long = 1000
    val MINUTE = SECOND * 60
    val HOUR = MINUTE * 60
    val DAY = HOUR * 24
    val YEAR = DAY * 365

    val WEEK = DAY * 7
    val TWO_WEEKS = WEEK * 2

    /**
     * Gets whether or not we are currently in the night time. This is defined as before 6 AM or
     * after 10 PM.
     */
    val isNight: Boolean
        get() = isNight(Calendar.getInstance())

    /**
     * If the next timestamp is more than 15 minutes away, we will display it on the message.
     *
     * @param timestamp     the current message's timestamp.
     * @param nextTimestamp the next message's timestamp. This should be larger than timestamp.
     * @return true if we should display the timestamp, false otherwise.
     */
    fun shouldDisplayTimestamp(timestamp: Long, nextTimestamp: Long): Boolean {
        return nextTimestamp >= timestamp + 15 * MINUTE
    }

    /**
     * Checks whether the timestamp is on the same calendar day as today.
     *
     * @param timestamp the timestamp to check.
     * @return true if same calendar day, false otherwise.
     */
    fun isToday(timestamp: Long): Boolean {
        return isToday(timestamp, now)
    }

    fun isToday(timestamp: Long, currentTime: Long): Boolean {
        val current = Calendar.getInstance()
        current.timeInMillis = currentTime
        zeroCalendarDay(current)

        val time = Calendar.getInstance()
        time.timeInMillis = timestamp
        zeroCalendarDay(time)

        return current.timeInMillis == time.timeInMillis
    }

    /**
     * Checks whether the timestamp is on the same calendar day as yesterday.
     *
     * @param timestamp the timestamp to check.
     * @return if if yesterday, false otherwise.
     */
    fun isYesterday(timestamp: Long): Boolean {
        return isYesterday(timestamp, now)
    }

    fun isYesterday(timestamp: Long, currentTime: Long): Boolean {
        val current = Calendar.getInstance()
        current.timeInMillis = currentTime
        zeroCalendarDay(current)
        current.set(Calendar.DAY_OF_YEAR, current.get(Calendar.DAY_OF_YEAR) - 1)

        val time = Calendar.getInstance()
        time.timeInMillis = timestamp
        zeroCalendarDay(time)

        return current.timeInMillis == time.timeInMillis
    }

    /**
     * Checks whether the timestamp is within the last week.
     */
    fun isLastWeek(timestamp: Long): Boolean {
        return isLastWeek(timestamp, now)
    }

    fun isLastWeek(timestamp: Long, currentTime: Long): Boolean {
        val lastWeek = Calendar.getInstance()
        lastWeek.timeInMillis = currentTime
        zeroCalendarDay(lastWeek)
        lastWeek.set(Calendar.WEEK_OF_YEAR, lastWeek.get(Calendar.WEEK_OF_YEAR) - 1)

        return timestamp > lastWeek.timeInMillis && timestamp < currentTime
    }

    /**
     * Checks whether the timestamp is within the last month.
     */
    fun isLastMonth(timestamp: Long): Boolean {
        return isLastMonth(timestamp, now)
    }

    fun isLastMonth(timestamp: Long, currentTime: Long): Boolean {
        val lastMonth = Calendar.getInstance()
        lastMonth.timeInMillis = currentTime
        zeroCalendarDay(lastMonth)

        // if the current month is January, then we need to set the last month to be December of the previous year
        if (lastMonth.get(Calendar.MONTH) == Calendar.JANUARY) {
            lastMonth.set(Calendar.YEAR, lastMonth.get(Calendar.YEAR) - 1)
            lastMonth.set(Calendar.MONTH, Calendar.DECEMBER)
        } else {
            lastMonth.set(Calendar.MONTH, lastMonth.get(Calendar.MONTH) - 1)
        }

        return timestamp > lastMonth.timeInMillis && timestamp < currentTime
    }

    fun today(): Long {
        val now = Calendar.getInstance()
        zeroCalendarDay(now)
        return now.timeInMillis
    }

    private fun zeroCalendarDay(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
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
    fun formatTimestamp(context: Context, timestamp: Long): String {
        return formatTimestamp(context, timestamp, TimeUtils.now)
    }

    fun formatTimestamp(context: Context, timestamp: Long, currentTime: Long): String {
        val date = Date(timestamp)
        val formatted: String?

        formatted = when {
            timestamp > currentTime - 2 * MINUTE -> context.getString(R.string.now)
            timestamp > today() -> formatTime(context, date)
            timestamp > currentTime - 7 * DAY -> SimpleDateFormat("E", Locale.getDefault()).format(date) + ", " +
                    formatTime(context, date)
            timestamp > currentTime - YEAR -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date) + ", " +
                    formatTime(context, date)
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date) + ", " +
                    formatTime(context, date)
        }

        return formatted ?: ""
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
    fun formatConversationTimestamp(context: Context, timestamp: Long, currentTime: Long = now): String {
        val date = Date(timestamp)
        val formatted: String?

        formatted = when {
            timestamp > today() -> formatTime(context, date)
            timestamp > currentTime - 7 * DAY -> SimpleDateFormat("E", Locale.getDefault()).format(date)
            timestamp > currentTime - YEAR -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)
        }

        return formatted ?: ""
    }

    fun formatTime(context: Context, date: Date): String {
        return if (android.text.format.DateFormat.is24HourFormat(context)) {
            android.text.format.DateFormat.format("HH:mm", date).toString()
        } else {
            android.text.format.DateFormat.format("h:mm a", date).toString()
        }
    }

    fun isNight(cal: Calendar): Boolean {
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour <= 5 || hour >= 20
    }

    fun setupNightTheme(activity: AppCompatActivity? = null, base: BaseTheme = Settings.baseTheme) {
        if (AndroidVersionUtil.isAndroidQ && base == BaseTheme.DAY_NIGHT) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            activity?.delegate?.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else if (base == BaseTheme.ALWAYS_LIGHT || (base == BaseTheme.DAY_NIGHT && !isNight)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            activity?.delegate?.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            activity?.delegate?.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    /**
     * How many seconds until the given hour, tomorrow.
     *
     * @param hour 24 hour format
     * @return seconds until that hour
     */
    fun millisUntilHourInTheNextDay(hour: Int): Long {
        return millisUntilHourInTheNextDay(hour, Calendar.getInstance().timeInMillis)
    }

    fun millisUntilHourInTheNextDay(hour: Int, currentTime: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTime)

        // force the calendar to 3 in the morning, on the next day.
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val lookingFor = calendar.timeInMillis

        return lookingFor - currentTime
    }
}
