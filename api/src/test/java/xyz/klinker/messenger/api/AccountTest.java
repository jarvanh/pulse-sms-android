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

import xyz.klinker.messenger.api.entity.AccountCountResponse;
import xyz.klinker.messenger.api.entity.AccountListResponse;
import xyz.klinker.messenger.api.entity.LoginRequest;
import xyz.klinker.messenger.api.entity.LoginResponse;
import xyz.klinker.messenger.api.entity.SignupResponse;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AccountTest extends ApiTest {

    @Test
    public void signUp() {
        SignupResponse response = getSignupResponse();

        if (response != null) {
            System.out.println(response);
            assertNotNull(response.accountId);
            assertNotNull(response.salt1);
            assertNotNull(response.salt2);
        } else {
            System.out.println("signup failed");
        }
    }

    @Test
    public void loginSuccessful() {
        LoginResponse response = getLoginResponse();

        System.out.println(response);
        assertNotNull(response);
        assertNotNull(response.accountId);
        assertNotNull(response.salt1);
        assertNotNull(response.salt2);
        assertNotNull(response.name);
        assertNotNull(response.phoneNumber);
    }

    @Test
    public void loginFailed() {
        LoginRequest request = new LoginRequest(USERNAME, "wrong " + PASSWORD);
        LoginResponse response = api.account().login(request);

        assertNull(response);
    }

    @Test
    public void count() {
        AccountCountResponse response = api.account().count(getAccountId());
        System.out.println(response);
        assertNotNull(response);
    }

    @Test
    public void list() {
        AccountListResponse response = api.account().list(getAccountId());
        System.out.println(response);
        assertNotNull(response);
    }

    @Test
    public void remove() {
        String accountId = getAccountId();
        Object response = api.account().remove(accountId);

        assertNotNull(response);
        assertNull(getLoginResponse());
    }

    @Test
    public void updateSettings() {
        String accountId = getAccountId();
        assertNotNull(api.account().updateSetting(accountId, "test", "boolean", true));
    }

    @Test
    public void dismissNotification() {
        assertNotNull(api.account().dismissedNotification(getAccountId(), "1", 1));
    }

    @Test
    public void viewSubscription() {
        assertNotNull(api.account().viewSubscription(getAccountId()));
    }

    @Test
    public void updateSubscription() {
        assertNotNull(api.account().updateSubscription(getAccountId(), 1, 1000));
    }
}
