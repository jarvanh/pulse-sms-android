package xyz.klinker.messenger.service;

import org.junit.Test;

import xyz.klinker.messenger.MessengerSuite;
import xyz.klinker.messenger.shared.data.model.Message;
import xyz.klinker.messenger.shared.service.NewMessagesCheckService;

import static org.junit.Assert.*;

public class NewMessageCheckServiceTest extends MessengerSuite {

    @Test
    public void typesMatch() {
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_RECEIVED, Message.TYPE_RECEIVED));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENT, Message.TYPE_SENT));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENDING, Message.TYPE_SENDING));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_ERROR, Message.TYPE_ERROR));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_DELIVERED, Message.TYPE_DELIVERED));
    }

    @Test
    public void typesDoNotMatch() {
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.TYPE_RECEIVED, Message.TYPE_SENDING));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.TYPE_RECEIVED, Message.TYPE_SENT));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.TYPE_RECEIVED, Message.TYPE_ERROR));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.TYPE_RECEIVED, Message.TYPE_DELIVERED));

        assertFalse(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENDING, Message.TYPE_RECEIVED));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENT, Message.TYPE_RECEIVED));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.TYPE_DELIVERED, Message.TYPE_RECEIVED));
        assertFalse(NewMessagesCheckService.typesAreEqual(Message.TYPE_ERROR, Message.TYPE_RECEIVED));
    }

    @Test
    public void newMessageSendingTypesAreEquivalent() {
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENT, Message.TYPE_SENDING));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENT, Message.TYPE_SENT));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENT, Message.TYPE_DELIVERED));

        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENDING, Message.TYPE_SENDING));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENDING, Message.TYPE_SENT));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENDING, Message.TYPE_DELIVERED));

        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_DELIVERED, Message.TYPE_SENDING));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_DELIVERED, Message.TYPE_SENT));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_DELIVERED, Message.TYPE_DELIVERED));
    }

    @Test
    public void oldMessageSendingTypesAreEquivalent() {
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENDING, Message.TYPE_SENT));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENT, Message.TYPE_SENT));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_DELIVERED, Message.TYPE_SENT));

        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENDING, Message.TYPE_SENDING));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENT, Message.TYPE_SENDING));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_DELIVERED, Message.TYPE_SENDING));

        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENDING, Message.TYPE_DELIVERED));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_SENT, Message.TYPE_DELIVERED));
        assertTrue(NewMessagesCheckService.typesAreEqual(Message.TYPE_DELIVERED, Message.TYPE_DELIVERED));
    }
}
