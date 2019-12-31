/*
 * Copyright (C) 2017 Luke Klinker
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

import xyz.klinker.messenger.api.Api;

/**
 * A helper for getting the correct API object.
 */
class ApiAccessor {

    public static Api create(String environment) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (environment) {
//            case "debug": return new Api(Api.Environment.DEBUG);
            case "staging": return new Api(Api.Environment.STAGING);
            default: return new Api(Api.Environment.RELEASE);
        }
    }

}
