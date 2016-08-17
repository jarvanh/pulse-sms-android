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

package xyz.klinker.messenger.api.implementation;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.R;

import static org.junit.Assert.assertEquals;

public class ApiUtilsTest extends MessengerRobolectricSuite {

    private ApiUtils apiUtils;

    @Before
    public void setUp() {
        apiUtils = new ApiUtils();
    }

    @Test
    public void baseUrl() {
        String url = apiUtils.getApi().baseUrl();
        String environment = RuntimeEnvironment.application.getString(R.string.environment);

        if (environment.equals("debug")) {
            assertEquals("http://192.168.1.127:3000/api/v1/", url);
        } else if (environment.equals("staging")) {
            assertEquals("https://klinkerapps-messenger-staging.herokuapp.com/api/v1/", url);
        } else {
            assertEquals("https://klinkerapps-messenger.herokuapp.com/api/v1/", url);
        }
    }

}
