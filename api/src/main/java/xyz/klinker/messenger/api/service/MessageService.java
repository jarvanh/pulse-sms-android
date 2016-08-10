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
import xyz.klinker.messenger.api.entity.AddMessagesRequest;
import xyz.klinker.messenger.api.entity.MessageBody;
import xyz.klinker.messenger.api.entity.UpdateMessageRequest;

public interface MessageService {

    @POST("messages/add")
    Object add(@Body AddMessagesRequest request);

    @POST("messages/update/{device_id}")
    Object update(@Path("device_id") int deviceId, @Query("account_id") String accountId,
                  @Body UpdateMessageRequest request);

    @GET("messages")
    MessageBody[] list(@Query("account_id") String accountId,
                       @Query("conversation_id") Integer conversationId,
                       @Query("limit") Integer limit,
                       @Query("offset") Integer offset);

}
