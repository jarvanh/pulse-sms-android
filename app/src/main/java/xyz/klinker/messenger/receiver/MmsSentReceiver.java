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

package xyz.klinker.messenger.receiver;

import android.content.Context;
import android.content.Intent;

/**
 * Receiver which gets a notification when an MMS message has finished sending. It will mark the
 * message as sent in the database by default. We also need to add functionality for marking it
 * as sent in our own database.
 */
public class MmsSentReceiver extends com.klinker.android.send_message.MmsSentReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // TODO mark message as sent in our database
    }

}
