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

package xyz.klinker.messenger.api.implementation;

import xyz.klinker.messenger.api.Api;
import xyz.klinker.messenger.api.entity.AddDeviceRequest;
import xyz.klinker.messenger.api.entity.AddDeviceResponse;
import xyz.klinker.messenger.api.entity.DeviceBody;
import xyz.klinker.messenger.api.entity.LoginRequest;
import xyz.klinker.messenger.api.entity.LoginResponse;
import xyz.klinker.messenger.api.entity.SignupRequest;
import xyz.klinker.messenger.api.entity.SignupResponse;

/**
 * Utility for easing access to APIs.
 */
public class ApiUtils {

    private Api api;

    public ApiUtils(String environment) {
        this.api = ApiAccessor.create(environment);
    }

    public LoginResponse login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        return api.account().login(request);
    }

    public SignupResponse signup(String email, String password, String name, String phoneNumber) {
        SignupRequest request = new SignupRequest(email, name, password, phoneNumber);
        return api.account().signup(request);
    }

    public Integer registerDevice(String accountId, String info, String name,
                                            boolean primary, String fcmToken) {
        DeviceBody deviceBody = new DeviceBody(info, name, primary, fcmToken);
        AddDeviceRequest request = new AddDeviceRequest(accountId, deviceBody);
        AddDeviceResponse response = api.device().add(request);

        if (response != null) {
            return response.id;
        } else {
            return null;
        }
    }

    public Api getApi() {
        return api;
    }

}
