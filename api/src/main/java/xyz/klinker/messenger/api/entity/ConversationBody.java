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

package xyz.klinker.messenger.api.entity;

public class ConversationBody {

    public int deviceId;
    public int color;
    public int colorDark;
    public int colorLight;
    public int colorAccent;
    public boolean pinned;
    public boolean read;
    public long timestamp;
    public String title;
    public String phoneNumbers;
    public String snippet;
    public String ringtone;
    public String imageUri;
    public String idMatcher;
    public boolean mute;

    public ConversationBody(int deviceId, int color, int colorDark, int colorLight, int colorAccent,
                            boolean pinned, boolean read, long timestamp, String title,
                            String phoneNumbers, String snippet, String ringtone, String imageUri,
                            String idMatcher, boolean mute) {
        this.deviceId = deviceId;
        this.color = color;
        this.colorDark = colorDark;
        this.colorLight = colorLight;
        this.colorAccent = colorAccent;
        this.pinned = pinned;
        this.read = read;
        this.timestamp = timestamp;
        this.title = title;
        this.phoneNumbers = phoneNumbers;
        this.snippet = snippet;
        this.ringtone = ringtone;
        this.imageUri = imageUri;
        this.idMatcher = idMatcher;
        this.mute = mute;
    }

    @Override
    public String toString() {
        return deviceId + ", " + color + ", " + colorDark + ", " + colorLight + ", " + colorAccent +
                ", " + pinned + ", " + read + ", " + timestamp + ", " + title + ", " +
                phoneNumbers + ", " + snippet + ", " + ringtone + ", " + imageUri + ", " +
                idMatcher + ", " + mute;
    }

}
