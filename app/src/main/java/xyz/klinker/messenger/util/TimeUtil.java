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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Helper for working with timestamps on messages.
 */
public class TimeUtil {

    /**
     * If the next timestamp is more than 15 minutes away, we will display it on the message.
     *
     * @param timestamp the current message's timestamp.
     * @param nextTimestamp the next message's timestamp.
     * @return true if we should display the timestamp, false otherwise.
     */
    public static boolean shouldDisplayTimestamp(long timestamp, long nextTimestamp) {
        return nextTimestamp >= timestamp + (1000 * 60 * 15);
    }

    /**
     * Formats the timestamp in a different way depending upon how long ago it was.
     *
     * @param timestamp the timestamp to format.
     * @return the formatted string.
     */
    public static String formatTimestamp(long timestamp) {
        SimpleDateFormat formatter = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return formatter.format(new Date(timestamp));
    }

}
