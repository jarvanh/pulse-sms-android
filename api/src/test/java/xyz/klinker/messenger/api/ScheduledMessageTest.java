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

package xyz.klinker.messenger.api;

import org.junit.Test;

import java.io.IOException;

import xyz.klinker.messenger.api.entity.AddScheduledMessageRequest;
import xyz.klinker.messenger.api.entity.ScheduledMessageBody;
import xyz.klinker.messenger.api.entity.UpdateScheduledMessageRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ScheduledMessageTest extends ApiTest {

    @Test
    public void addAndUpdateAndRemove() throws IOException {
        String accountId = getAccountId();
        int originalSize = api.scheduled().list(accountId).execute().body().length;

        ScheduledMessageBody message = new ScheduledMessageBody(1, "5159911493", "test",
                "text/plain", System.currentTimeMillis(), "test");
        AddScheduledMessageRequest request = new AddScheduledMessageRequest(accountId, message);
        Object response = api.scheduled().add(request).execute().body();
        assertNotNull(response);

        UpdateScheduledMessageRequest update = new UpdateScheduledMessageRequest("5154224558",
                null, null, null, null);
        api.scheduled().update(1, accountId, update).execute();

        ScheduledMessageBody[] messages = api.scheduled().list(accountId).execute().body();
        assertEquals(1, messages.length - originalSize);

        api.scheduled().remove(1, accountId).execute();

        messages = api.scheduled().list(accountId).execute().body();
        assertEquals(messages.length, originalSize);
    }

}
