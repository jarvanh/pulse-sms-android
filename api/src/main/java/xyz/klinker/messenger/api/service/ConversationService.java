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

package xyz.klinker.messenger.api.service;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import xyz.klinker.messenger.api.entity.AddConversationRequest;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.UpdateConversationRequest;

public interface ConversationService {

    @POST("conversations/add")
    Object add(@Body AddConversationRequest request);

    @POST("conversations/update/{device_id}")
    Object update(@Path("device_id") long deviceId, @Query("account_id") String accountId,
                  @Body UpdateConversationRequest request);

    @POST("conversations/update_snippet/{device_id}")
    Object updateSnippet(@Path("device_id") long deviceId, @Query("account_id") String accountId,
                         @Body UpdateConversationRequest request);

    @POST("conversations/update_title/{device_id}")
    Object updateTitle(@Path("device_id") long deviceId, @Query("account_id") String accountId,
                       @Query("title") String title);

    @POST("conversations/remove/{device_id}")
    Object remove(@Path("device_id") long deviceId, @Query("account_id") String accountId);

    @GET("conversations")
    ConversationBody[] list(@Query("account_id") String accountId);

    @POST("conversations/read/{device_id}")
    Object read(@Path("device_id") long deviceId, @Query("android_device") String androidDeviceId, @Query("account_id") String accountId);

    @POST("conversations/seen/{device_id}")
    Object seen(@Path("device_id") long deviceId, @Query("account_id") String accountId);

    @POST("conversations/seen")
    Object seen(@Query("account_id") String accountId);

    @POST("conversations/archive/{device_id}")
    Object archive(@Path("device_id") long deviceId, @Query("account_id") String accountId);

    @POST("conversations/unarchive/{device_id}")
    Object unarchive(@Path("device_id") long deviceId, @Query("account_id") String accountId);

}
