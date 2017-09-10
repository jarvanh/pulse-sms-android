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

import xyz.klinker.messenger.api.entity.AddDeviceRequest;
import xyz.klinker.messenger.api.entity.AddDeviceResponse;
import xyz.klinker.messenger.api.entity.DeviceBody;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeviceTest extends ApiTest {

    @Test
    public void addAndUpdateAndRemoveDevice() throws IOException {
        String accountId = getAccountId();

        DeviceBody device = new DeviceBody("test", "test device", true, "1");
        AddDeviceRequest request = new AddDeviceRequest(accountId, device);
        AddDeviceResponse response = api.device().add(request).execute().body();
        assertNotNull(response);

        DeviceBody[] devices = api.device().list(accountId).execute().body();
        assertEquals(1, devices.length);
        assertEquals("test device", devices[0].name);

        api.device().update(response.id, accountId, "test device 2", null).execute();
        api.device().update(response.id, accountId, null, "3").execute();

        devices = api.device().list(accountId).execute().body();
        assertEquals(1, devices.length);
        assertEquals("test device 2", devices[0].name);

        api.device().remove(response.id, accountId).execute();

        devices = api.device().list(accountId).execute().body();
        assertEquals(0, devices.length);
    }

}
