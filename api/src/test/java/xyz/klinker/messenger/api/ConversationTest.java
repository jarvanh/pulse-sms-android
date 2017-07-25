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

import java.io.IOException;

import xyz.klinker.messenger.api.entity.AddConversationRequest;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.UpdateConversationRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConversationTest extends ApiTest {

    @Test
    public void addAndUpdateAndRemove() throws IOException {
        String accountId = getAccountId();
        int originalSize = api.conversation().list(accountId).execute().body().length;

        ConversationBody draft = new ConversationBody(1, 1, 1, 1, 1, 1, true, false,
                System.currentTimeMillis(), "test", "5154224558", "hey!", null, null,
                "24558", false, false, false);
        AddConversationRequest request = new AddConversationRequest(accountId, draft);
        Object response = api.conversation().add(request).execute().body();
        assertNotNull(response);

        UpdateConversationRequest update = new UpdateConversationRequest(null, null, null, null, null,
                false, true, null, null, null, null, null, null, null);
        api.conversation().update(1, accountId, update).execute();

        ConversationBody[] conversations = api.conversation().list(accountId).execute().body();
        assertEquals(1, conversations.length - originalSize);

        api.conversation().remove(1, accountId).execute();

        conversations = api.conversation().list(accountId).execute().body();
        assertEquals(conversations.length, originalSize);

        assertNotNull(api.conversation().read(1, null, accountId).execute().body());
        assertNotNull(api.conversation().seen(1, accountId).execute().body());
        assertNotNull(api.conversation().seen(accountId).execute().body());
    }

}
