package xyz.klinker.messenger.service.jobs

import org.junit.Test
import xyz.klinker.messenger.MessengerSuite

import org.junit.Assert.*

class ScheduledMessageJobTest : MessengerSuite() {

    @Test
    fun ordersMessagesWithClosestTimestampFirst() {
        val list = listOf(4, 5, 1, 3, 4, 7, 6)
                .sortedBy { it }

        assertEquals(list, listOf(1,3,4,4,5,6,7))
    }
}