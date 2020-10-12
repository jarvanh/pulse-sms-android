package xyz.klinker.messenger.shared.util;

import org.junit.Test;

import xyz.klinker.messenger.MessengerSuite;
import xyz.klinker.messenger.shared.data.model.Conversation;

import static org.junit.Assert.*;

public class ContactUtilsTest extends MessengerSuite {

    @Test
    public void shouldConvertLongNumberToPlain() {
        assertEquals("5159911493", ContactUtils.INSTANCE.getPlainNumber("+15159911493"));
        assertEquals("5159911493", ContactUtils.INSTANCE.getPlainNumber("+5159911493"));
        assertEquals("5159911493", ContactUtils.INSTANCE.getPlainNumber("15159911493"));
        assertEquals("5159911493", ContactUtils.INSTANCE.getPlainNumber("5159911493"));
        assertEquals("9911493", ContactUtils.INSTANCE.getPlainNumber("9911493"));
    }

    @Test
    public void shouldShowImageLetter() {
        Conversation conversation = new Conversation();
        conversation.setPhoneNumbers("555");

        conversation.setTitle("test");
        assertTrue(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));

        conversation.setTitle("Test");
        assertTrue(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));

        conversation.setTitle("Żółć");
        assertTrue(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));

        conversation.setTitle("Ὀδυσσεύς");
        assertTrue(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));

        conversation.setTitle("原田雅彦");
        assertTrue(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));
    }

    @Test
    public void shouldNotShowImageLetter() {
        Conversation conversation = new Conversation();
        conversation.setPhoneNumbers("1, 1");

        conversation.setTitle(" test");
        assertFalse(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));

        conversation.setTitle("5.");
        assertFalse(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));

        conversation.setTitle(".j4");
        assertFalse(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));

        conversation.setTitle("+1");
        assertFalse(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));

        conversation.setTitle("-");
        assertFalse(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));

        conversation.setTitle("'");
        assertFalse(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));

        conversation.setTitle(":");
        assertFalse(ContactUtils.INSTANCE.shouldDisplayContactLetter(conversation));
    }
}