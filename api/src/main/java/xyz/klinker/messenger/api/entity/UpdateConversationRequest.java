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

public class UpdateConversationRequest {

    public Integer color;
    public Integer colorDark;
    public Integer colorLight;
    public Integer colorAccent;
    public Boolean pinned;
    public Boolean read;
    public Long timestamp;
    public String title;
    public String snippet;
    public String ringtone;
    public Boolean mute;

    public UpdateConversationRequest(Integer color, Integer colorDark, Integer colorLight,
                                     Integer colorAccent, Boolean pinned, Boolean read,
                                     Long timestamp, String title, String snippet, String ringtone,
                                     Boolean mute) {
        this.color = color;
        this.colorDark = colorDark;
        this.colorLight = colorLight;
        this.colorAccent = colorAccent;
        this.pinned = pinned;
        this.read = read;
        this.timestamp = timestamp;
        this.title = title;
        this.snippet = snippet;
        this.ringtone = ringtone;
        this.mute = mute;
    }

}
