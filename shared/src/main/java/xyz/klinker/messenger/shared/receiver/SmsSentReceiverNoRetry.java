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

package xyz.klinker.messenger.shared.receiver;

/**
 * Receiver for getting notifications of when an SMS has finished sending. By default it's super
 * class will mark the internal message as sent, we need to also mark our database as sent.
 */
public class SmsSentReceiverNoRetry extends SmsSentReceiver {

    protected boolean retryFailedMessages() {
        return false;
    }
}
