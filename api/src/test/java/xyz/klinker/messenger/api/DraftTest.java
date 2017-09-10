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

import xyz.klinker.messenger.api.entity.AddDraftRequest;
import xyz.klinker.messenger.api.entity.DraftBody;
import xyz.klinker.messenger.api.entity.UpdateDraftRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DraftTest extends ApiTest {

    @Test
    public void addAndUpdateAndRemove() throws IOException {
        String accountId = getAccountId();
        int originalSize = api.draft().list(accountId).execute().body().length;

        DraftBody draft = new DraftBody(1, 1, "test draft", "text/plain");
        AddDraftRequest request = new AddDraftRequest(accountId, draft);
        Object response = api.draft().add(request).execute().body();
        assertNotNull(response);

        UpdateDraftRequest update = new UpdateDraftRequest("new test draft", null);
        api.draft().update(1, accountId, update).execute().body();

        DraftBody[] drafts = api.draft().list(accountId).execute().body();
        assertEquals(1, drafts.length - originalSize);

        api.draft().remove(1, null, accountId).execute();

        drafts = api.draft().list(accountId).execute().body();
        assertEquals(drafts.length, originalSize);
    }

}
