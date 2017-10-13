package xyz.klinker.messenger.util;

import org.junit.Test;
import org.mockito.Mock;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.shared.data.MmsSettings;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.util.MessageCountHelper;

import static org.junit.Assert.*;

public class MessageCountHelperTest extends MessengerRobolectricSuite {

    // 4 characters
    private static final String ONE_MESSAGE = "test";

    // 132 characters
    private static final String ONE_LONGER_MESSAGE = "testing one two three four five six seven eight " +
            "nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen";

    // 265 characters
    private static final String TWO_MESSAGES = "testing one two three four five six seven eight nine " +
            "ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen nineteen testing " +
            "one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen " +
            "sixteen seventeen eighteen nineteen";

    @Mock
    private MmsSettings mmsSettings;
    @Mock
    private Settings settings;

    @Test
    public void neverConvertToMMS() {
        mmsSettings.setConvertLongMessagesToMMS(false);

        assertEquals(null, MessageCountHelper.getMessageCounterText(settings, mmsSettings, ONE_MESSAGE));
        assertEquals("1/28", MessageCountHelper.getMessageCounterText(settings, mmsSettings, ONE_LONGER_MESSAGE));
        assertEquals("2/41", MessageCountHelper.getMessageCounterText(settings, mmsSettings, TWO_MESSAGES));
    }

    @Test
    public void withConvertingToMMSAfterOneMessage() {
        mmsSettings.setConvertLongMessagesToMMS(true);
        mmsSettings.setNumberOfMessagesBeforeMms(1);

        assertEquals(null, MessageCountHelper.getMessageCounterText(settings, mmsSettings, ONE_MESSAGE));
        assertEquals("1/28", MessageCountHelper.getMessageCounterText(settings, mmsSettings, ONE_LONGER_MESSAGE));
        assertEquals(null, MessageCountHelper.getMessageCounterText(settings, mmsSettings, TWO_MESSAGES));
        assertEquals(null, MessageCountHelper.getMessageCounterText(settings, mmsSettings, TWO_MESSAGES + TWO_MESSAGES));
    }

    @Test
    public void withConvertingToMMSAfterTwoMessages() {
        mmsSettings.setConvertLongMessagesToMMS(true);
        mmsSettings.setNumberOfMessagesBeforeMms(2);

        assertEquals(null, MessageCountHelper.getMessageCounterText(settings, mmsSettings, ONE_MESSAGE));
        assertEquals("1/28", MessageCountHelper.getMessageCounterText(settings, mmsSettings, ONE_LONGER_MESSAGE));
        assertEquals("2/41", MessageCountHelper.getMessageCounterText(settings, mmsSettings, TWO_MESSAGES));
        assertEquals(null, MessageCountHelper.getMessageCounterText(settings, mmsSettings, TWO_MESSAGES + ONE_LONGER_MESSAGE));
    }

    @Test
    public void withConvertingToMMSAfterThreeMessages() {
        mmsSettings.setConvertLongMessagesToMMS(true);
        mmsSettings.setNumberOfMessagesBeforeMms(3);

        assertEquals(null, MessageCountHelper.getMessageCounterText(settings, mmsSettings, ONE_MESSAGE));
        assertEquals("1/28", MessageCountHelper.getMessageCounterText(settings, mmsSettings, ONE_LONGER_MESSAGE));
        assertEquals("2/41", MessageCountHelper.getMessageCounterText(settings, mmsSettings, TWO_MESSAGES));
        assertEquals("3/62", MessageCountHelper.getMessageCounterText(settings, mmsSettings, TWO_MESSAGES + ONE_LONGER_MESSAGE));
        assertEquals(null, MessageCountHelper.getMessageCounterText(settings, mmsSettings, TWO_MESSAGES + TWO_MESSAGES));
    }
}