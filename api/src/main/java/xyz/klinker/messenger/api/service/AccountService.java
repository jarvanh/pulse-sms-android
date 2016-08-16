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
    SignupResponse signup(@Body SignupRequest request);

    @POST("accounts/login")
    LoginResponse login(@Body LoginRequest request);

    @POST("accounts/remove/{account_id}")
    Object remove(@Path("account_id") String accountId);

    @GET("accounts/count")
    AccountCountResponse count(@Query("account_id") String accountId);

    @GET("accounts")
    AccountListResponse list(@Query("account_id") String accountId);

    @POST("accounts/update_snooze")
    Object updateSnooze(@Query("account_id") String accountId, @Query("snooze") long snooze);

    @POST("accounts/update_dark_theme")
    Object updateDarkTheme(@Query("account_id") String accountId,
                           @Query("dark_theme") boolean darkTheme);

    @POST("accounts/update_vibrate")
    Object updateVibrate(@Query("account_id") String accountId, @Query("vibrate") boolean vibrate);

    @POST("accounts/dismissed_notification")
    Object dismissedNotification(@Query("account_id") String accountId, @Query("id") long id);

}
