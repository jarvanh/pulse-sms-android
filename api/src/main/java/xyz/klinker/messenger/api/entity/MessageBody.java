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

public class MessageBody {

    public long deviceId;
    public long deviceConversationId;
    public int messageType;
    public String data;
    public long timestamp;
    public String mimeType;
    public boolean read;
    public boolean seen;
    public String messageFrom;
    public Integer color;

    public MessageBody(long deviceId, long deviceConversationId, int messageType, String data,
                       long timestamp, String mimeType, boolean read, boolean seen,
                       String messageFrom, Integer color) {
        this.deviceId = deviceId;
        this.deviceConversationId = deviceConversationId;
        this.messageType = messageType;
        this.data = data;
        this.timestamp = timestamp;
        this.mimeType = mimeType;
        this.read = read;
        this.seen = seen;
        this.messageFrom = messageFrom;
        this.color = color;
    }

    @Override
    public String toString() {
        return deviceId + ", " + deviceConversationId + ", " + messageType + ", " + data + ", " +
                timestamp + ", " + mimeType + ", " + read + ", " + seen + ", " + messageFrom
                + ", " + color;
    }
}
