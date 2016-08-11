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

package xyz.klinker.messenger.api;

import org.junit.Test;

import xyz.klinker.messenger.api.entity.AddScheduledMessageRequest;
import xyz.klinker.messenger.api.entity.ScheduledMessageBody;
import xyz.klinker.messenger.api.entity.UpdateScheduledMessageRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ScheduledMessageTest extends ApiTest {

    @Test
    public void addAndUpdateAndRemove() {
        String accountId = getAccountId();
        int originalSize = api.scheduled().list(accountId).length;

        ScheduledMessageBody message = new ScheduledMessageBody(1, "5159911493", "test",
                "text/plain", System.currentTimeMillis());
        AddScheduledMessageRequest request = new AddScheduledMessageRequest(accountId, message);
        Object response = api.scheduled().add(request);
        assertNotNull(response);

        UpdateScheduledMessageRequest update = new UpdateScheduledMessageRequest("5154224558",
                null, null, null);
        api.scheduled().update(1, accountId, update);

        ScheduledMessageBody[] messages = api.scheduled().list(accountId);
        assertEquals(1, messages.length - originalSize);

        api.scheduled().remove(1, accountId);

        messages = api.scheduled().list(accountId);
        assertEquals(messages.length, originalSize);
    }

}
