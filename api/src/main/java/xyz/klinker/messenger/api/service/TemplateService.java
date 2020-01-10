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
import xyz.klinker.messenger.api.entity.AddBlacklistRequest;
import xyz.klinker.messenger.api.entity.AddTemplateRequest;
import xyz.klinker.messenger.api.entity.BlacklistBody;
import xyz.klinker.messenger.api.entity.TemplateBody;
import xyz.klinker.messenger.api.entity.UpdateScheduledMessageRequest;
import xyz.klinker.messenger.api.entity.UpdateTemplateRequest;

public interface TemplateService {

    @POST("templates/add")
    Call<Void> add(@Body AddTemplateRequest request);

    @POST("templates/remove/{device_id}")
    Call<Void> remove(@Path("device_id") long deviceId, @Query("account_id") String accountId);

    @POST("templates/update/{device_id}")
    Call<Void> update(@Path("device_id") long deviceId, @Query("account_id") String accountId,
                      @Body UpdateTemplateRequest request);

    @GET("templates")
    Call<TemplateBody[]> list(@Query("account_id") String accountId);
}
