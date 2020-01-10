/*
 * Copyright (C) 2020 Luke Klinker
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

package xyz.klinker.messenger.api.service;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import xyz.klinker.messenger.api.entity.AddDeviceRequest;
import xyz.klinker.messenger.api.entity.AddDeviceResponse;
import xyz.klinker.messenger.api.entity.DeviceBody;

public interface DeviceService {

    @POST("devices/add")
    Call<AddDeviceResponse> add(@Body AddDeviceRequest request);

    @POST("devices/update/{id}")
    Call<Void> update(@Path("id") long id, @Query("account_id") String accountId,
                  @Query("name") String name, @Query("fcm_token") String fcmToken);

    @POST("devices/remove/{id}")
    Call<Void> remove(@Path("id") int id, @Query("account_id") String accountId);

    @POST("devices/update_primary")
    Call<Void> updatePrimary(@Query("new_primary_device_id") String newPrimaryDeviceId,
                         @Query("account_id") String accountId);

    @GET("devices")
    Call<DeviceBody[]> list(@Query("account_id") String accountId);

}
