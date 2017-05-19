package xyz.klinker.messenger.util;

import org.junit.Test;

import xyz.klinker.messenger.MessengerSuite;
import xyz.klinker.messenger.shared.util.MessageListRefreshMonitor;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class MessageListRefreshMonitorTest extends MessengerSuite {

    @Test
    public void shouldAllowRefreshImmediately() {
        assertTrue(new MessageListRefreshMonitor().shouldLoadMessagesToTheUi());
    }

    @Test
    public void shouldNotRefreshTheUiWhenAnotherThreadIsComingToOverwriteIt() {
        MessageListRefreshMonitor monitor = new MessageListRefreshMonitor();

        // two threads started very close together
        monitor.incrementRefreshThreadsCount();
        monitor.incrementRefreshThreadsCount();

        // first finishes
        monitor.decrementRefreshThreadsCount();

        // we don't want to refresh the UI here, because the next one is about to finish and
        // will just override any changes this one makes, immediately
        assertFalse(monitor.shouldLoadMessagesToTheUi());

        // second one finishes and we load the UI changes
        monitor.decrementRefreshThreadsCount();
        assertTrue(monitor.shouldLoadMessagesToTheUi());
    }
}
