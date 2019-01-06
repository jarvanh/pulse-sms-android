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

package xyz.klinker.messenger.api.entity;

public class BlacklistBody {

    public long deviceId;
    public String phoneNumber;
    public String phrase;

    public BlacklistBody(long deviceId, String phoneNumber, String phrase) {
        this.deviceId = deviceId;
        this.phoneNumber = phoneNumber;
        this.phrase = phrase;
    }

    @Override
    public String toString() {
        return deviceId + ", " + phoneNumber + ", " + phrase;
    }

}
