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
import xyz.klinker.messenger.api.entity.AccountCountResponse;
import xyz.klinker.messenger.api.entity.AccountListResponse;
import xyz.klinker.messenger.api.entity.LoginRequest;
import xyz.klinker.messenger.api.entity.LoginResponse;
import xyz.klinker.messenger.api.entity.SignupRequest;
import xyz.klinker.messenger.api.entity.SignupResponse;

/**
 * Service for interfacing with account endpoints.
 */
public interface AccountService {

    @POST("accounts/signup")
    Call<SignupResponse> signup(@Body SignupRequest request);

    @POST("accounts/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("accounts/remove_account")
    Call<Void> remove(@Query("account_id") String accountId);

    @POST("accounts/clean_account")
    Call<Void> clean(@Query("account_id") String accountId);

    @GET("accounts/count")
    Call<AccountCountResponse> count(@Query("account_id") String accountId);

    @GET("accounts")
    Call<AccountListResponse> list(@Query("account_id") String accountId);

    @POST("accounts/update_setting")
    Call<Void> updateSetting(@Query("account_id") String accountId, @Query("pref") String pref, @Query("type") String type, @Query("value") Object value);

    @POST("accounts/dismissed_notification")
    Call<Void> dismissedNotification(@Query("account_id") String accountId, @Query("device_id") String deviceId, @Query("id") long conversationId);

    @GET("accounts/view_subscription")
    Call<AccountListResponse> viewSubscription(@Query("account_id") String accountId);

    @POST("accounts/update_subscription")
    Call<Void> updateSubscription(@Query("account_id") String accountId,
                              @Query("subscription_type") int subscriptionType,
                              @Query("subscription_expiration") long subscriptionExpiration);
}
