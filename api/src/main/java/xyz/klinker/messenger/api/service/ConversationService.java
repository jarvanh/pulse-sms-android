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
import xyz.klinker.messenger.api.entity.AddConversationRequest;
import xyz.klinker.messenger.api.entity.ConversationBody;
import xyz.klinker.messenger.api.entity.UpdateConversationRequest;

public interface ConversationService {

    @POST("conversations/add")
    Call<Void> add(@Body AddConversationRequest request);

    @POST("conversations/update/{device_id}")
    Call<Void> update(@Path("device_id") long deviceId, @Query("account_id") String accountId,
                  @Body UpdateConversationRequest request);

    @POST("conversations/update_snippet/{device_id}")
    Call<Void> updateSnippet(@Path("device_id") long deviceId, @Query("account_id") String accountId,
                         @Body UpdateConversationRequest request);

    @POST("conversations/update_title/{device_id}")
    Call<Void> updateTitle(@Path("device_id") long deviceId, @Query("account_id") String accountId,
                       @Query("title") String title);

    @POST("conversations/remove/{device_id}")
    Call<Void> remove(@Path("device_id") long deviceId, @Query("account_id") String accountId);

    @GET("conversations")
    Call<ConversationBody[]> list(@Query("account_id") String accountId);

    @GET("conversations")
    Call<ConversationBody[]> list(@Query("account_id") String accountId, @Query("limit") Integer limit);

    @GET("conversations")
    Call<ConversationBody[]> list(@Query("account_id") String accountId, @Query("limit") Integer limit, @Query("offset") Integer offset);

    @POST("conversations/read/{device_id}")
    Call<Void> read(@Path("device_id") long deviceId, @Query("android_device") String androidDeviceId, @Query("account_id") String accountId);

    @POST("conversations/seen/{device_id}")
    Call<Void> seen(@Path("device_id") long deviceId, @Query("account_id") String accountId);

    @POST("conversations/seen")
    Call<Void> seen(@Query("account_id") String accountId);

    @POST("conversations/archive/{device_id}")
    Call<Void> archive(@Path("device_id") long deviceId, @Query("account_id") String accountId);

    @POST("conversations/unarchive/{device_id}")
    Call<Void> unarchive(@Path("device_id") long deviceId, @Query("account_id") String accountId);

    @POST("conversations/add_to_folder/{device_id}")
    Call<Void> addToFolder(@Path("device_id") long deviceId, @Query("folder_id") long folderId, @Query("account_id") String accountId);

    @POST("conversations/remove_from_folder/{device_id}")
    Call<Void> removeFromFolder(@Path("device_id") long deviceId, @Query("account_id") String accountId);

    @POST("conversations/clean")
    Call<Void> clean(@Query("account_id") String accountId);

    @POST("conversations/cleanup_messages")
    Call<Void> cleanup(@Query("account_id") String accountId, @Query("conversation_id") long conversationId, @Query("timestamp") long timestamp);

}
