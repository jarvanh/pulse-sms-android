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

import xyz.klinker.messenger.api.entity.SignupRequest;
import xyz.klinker.messenger.api.entity.SignupResponse;

import static org.junit.Assert.assertNotNull;

public class AccountTest extends ApiTest {

    @Test
    public void signUp() {
        SignupRequest request = new SignupRequest("test@email.com", "test user",
                "test password", "test");
        SignupResponse response = api.account().signup(request);

        if (response != null) {
            System.out.println(response);
            assertNotNull(response.accountId);
            assertNotNull(response.salt1);
            assertNotNull(response.salt2);
        } else {
            System.out.println("request failed");
        }
    }

}
