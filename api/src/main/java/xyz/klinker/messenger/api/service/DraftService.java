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
import xyz.klinker.messenger.api.entity.AddDraftRequest;
import xyz.klinker.messenger.api.entity.DraftBody;
import xyz.klinker.messenger.api.entity.UpdateDraftRequest;

public interface DraftService {

    @POST("drafts/add")
    Call<Void> add(@Body AddDraftRequest request);

    @POST("drafts/update/{device_id}")
    Call<Void> update(@Path("device_id") long deviceId, @Query("account_id") String accountId,
                  @Body UpdateDraftRequest request);

    @POST("drafts/remove/{device_conversation_id}")
    Call<Void> remove(@Path("device_conversation_id") long deviceConversationId, @Query("android_device") String androidDevice,
                  @Query("account_id") String accountId);

    @GET("drafts")
    Call<DraftBody[]> list(@Query("account_id") String accountId);

}
