package xyz.klinker.messenger.service;

import org.junit.Test;

import xyz.klinker.messenger.MessengerSuite;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.service.NewMessagesCheckService;

import static org.junit.Assert.*;

public class NewMessageCheckServiceTest extends MessengerSuite {

    @Test
    public void typesMatch() {
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_RECEIVED(), Message.Companion.getTYPE_RECEIVED()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENT(), Message.Companion.getTYPE_SENT()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENDING(), Message.Companion.getTYPE_SENDING()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_ERROR(), Message.Companion.getTYPE_ERROR()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_DELIVERED(), Message.Companion.getTYPE_DELIVERED()));
    }

    @Test
    public void typesDoNotMatch() {
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_RECEIVED(), Message.Companion.getTYPE_SENDING()));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_RECEIVED(), Message.Companion.getTYPE_SENT()));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_RECEIVED(), Message.Companion.getTYPE_ERROR()));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_RECEIVED(), Message.Companion.getTYPE_DELIVERED()));

        assertFalse(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENDING(), Message.Companion.getTYPE_RECEIVED()));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENT(), Message.Companion.getTYPE_RECEIVED()));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_DELIVERED(), Message.Companion.getTYPE_RECEIVED()));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_ERROR(), Message.Companion.getTYPE_RECEIVED()));
    }

    @Test
    public void newMessageSendingTypesAreEquivalent() {
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENT(), Message.Companion.getTYPE_SENDING()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENT(), Message.Companion.getTYPE_SENT()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENT(), Message.Companion.getTYPE_DELIVERED()));

        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENDING(), Message.Companion.getTYPE_SENDING()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENDING(), Message.Companion.getTYPE_SENT()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENDING(), Message.Companion.getTYPE_DELIVERED()));

        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_DELIVERED(), Message.Companion.getTYPE_SENDING()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_DELIVERED(), Message.Companion.getTYPE_SENT()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_DELIVERED(), Message.Companion.getTYPE_DELIVERED()));
    }

    @Test
    public void oldMessageSendingTypesAreEquivalent() {
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENDING(), Message.Companion.getTYPE_SENT()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENT(), Message.Companion.getTYPE_SENT()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_DELIVERED(), Message.Companion.getTYPE_SENT()));

        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENDING(), Message.Companion.getTYPE_SENDING()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENT(), Message.Companion.getTYPE_SENDING()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_DELIVERED(), Message.Companion.getTYPE_SENDING()));

        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENDING(), Message.Companion.getTYPE_DELIVERED()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_SENT(), Message.Companion.getTYPE_DELIVERED()));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.Companion.getTYPE_DELIVERED(), Message.Companion.getTYPE_DELIVERED()));
    }
}
