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

package xyz.klinker.messenger.api.entity;

public class ScheduledMessageBody {

    public long deviceId;
    public String to;
    public String data;
    public String mimeType;
    public long timestamp;
    public String title;
    public int repeat;

    public ScheduledMessageBody(long deviceId, String to, String data, String mimeType,
                                long timestamp, String title, int repeat) {
        this.deviceId = deviceId;
        this.to = to;
        this.data = data;
        this.mimeType = mimeType;
        this.timestamp = timestamp;
        this.title = title;
        this.repeat = repeat;
    }

    @Override
    public String toString() {
        return deviceId + ", " + to + ", " + data + ", " + mimeType + ", " + timestamp + ", " + repeat + ", " +
                title;
    }

}
