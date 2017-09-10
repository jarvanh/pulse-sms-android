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

import org.junit.Before;

import java.io.IOException;

import xyz.klinker.messenger.api.entity.LoginRequest;
import xyz.klinker.messenger.api.entity.LoginResponse;
import xyz.klinker.messenger.api.entity.SignupRequest;
import xyz.klinker.messenger.api.entity.SignupResponse;

public class ApiTest {

    public static final String USERNAME = "test@klinkerapps.com";
    public static final String PASSWORD = "test password";

    public Api api;

    @Before
    public void setUp() {
        api = new Api(Api.Environment.DEBUG);
    }

    public SignupResponse getSignupResponse() {
        try {
            SignupRequest request = new SignupRequest(USERNAME, "test user",
                    PASSWORD, "test");
            return api.account().signup(request).execute().body();
        } catch (IOException e) {
            return null;
        }
    }

    public LoginResponse getLoginResponse() {
        try {
            LoginRequest request = new LoginRequest(USERNAME, PASSWORD);
            return api.account().login(request).execute().body();
        } catch (IOException e) {
            return null;
        }
    }

    public String getAccountId() {
        SignupResponse signup = getSignupResponse();

        if (signup == null) {
            LoginResponse login = getLoginResponse();
            return login.accountId;
        } else {
            return signup.accountId;
        }
    }

}
