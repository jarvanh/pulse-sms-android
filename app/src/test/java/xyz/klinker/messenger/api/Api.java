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

package xyz.klinker.messenger.api;

/**
 * Duplicating this class here without the retrofit/okhttp stuff. For some reason, Robolectric
 * could not find those classes when running tests in Android Studio, so it was leading to failures
 * whenever a class interfaced with the api. Some type of dependency issue I could not solve.
 */
public class Api {
    private static final String API_DEBUG_URL = "http://192.168.1.127:3000/api/v1/";
    private static final String API_STAGING_URL = "https://klinkerapps-messenger-staging.herokuapp.com/api/v1/";
    private static final String API_RELEASE_URL = "https://klinkerapps-messenger.herokuapp.com/api/v1/";

    public enum Environment {
        DEBUG, STAGING, RELEASE
    }

    private String baseUrl;

    /**
     * Creates a new API access object that will connect to the correct environment.
     *
     * @param environment the Environment to use to connect to the APIs.
     */
    public Api(Api.Environment environment) {
        this(environment == Api.Environment.DEBUG ? API_DEBUG_URL :
                (environment == Api.Environment.STAGING ? API_STAGING_URL : API_RELEASE_URL));
    }

    private Api(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String baseUrl() {
        return baseUrl;
    }

}
