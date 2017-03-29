package xyz.klinker.messenger.shared.util;

import android.telephony.SmsMessage;

import com.klinker.android.send_message.StripAccents;

import xyz.klinker.messenger.shared.data.MmsSettings;
import xyz.klinker.messenger.shared.data.Settings;

public class MessageCountHelper {

    /**
     * Returns null if the message counter should be hidden, or the count (Ex: 1 / 17) if the count
     * should be shown.
     *
     * @param settings the settings for the app
     * @param mmsSettings the MMS settings for the app
     * @param text the text that the user is sending
     *
     * @return a string to be applied to the counter TextView
     */
    public static String getMessageCounterText(Settings settings, MmsSettings mmsSettings, String text) {
        boolean stripUnicode = settings.stripUnicode;
        boolean convertToMMS = mmsSettings.convertLongMessagesToMMS;
        int convertAfterXMessages = mmsSettings.numberOfMessagesBeforeMms;
        int numberOfMessages = 0;
        int charRemaining = 0;

        if (stripUnicode) {
            text = StripAccents.stripAccents(text);
        }

        try {
            int[] count = SmsMessage.calculateLength(text, false);
            numberOfMessages = count[0];
            charRemaining = count[2];
        } catch (Exception e) {
            return null;
        }

        // when they are running out of room on their first message,
        // we ALWAYS want to display this
        if (numberOfMessages == 1 && charRemaining < 30) {
            return formatCount(numberOfMessages, charRemaining);
        }

        if (!convertToMMS) {
            if (numberOfMessages > 1) {
                return formatCount(numberOfMessages, charRemaining);
            } else {
                return null;
            }
        }

        if (numberOfMessages > 1 && numberOfMessages <= convertAfterXMessages) {
            return formatCount(numberOfMessages, charRemaining);
        } else {
            return null;
        }
    }

    private static String formatCount(int numberOfMessages, int charRemaining) {
        return numberOfMessages + "/" + charRemaining;
    }
}
