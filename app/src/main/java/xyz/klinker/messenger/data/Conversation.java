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

package xyz.klinker.messenger.data;

import android.content.Context;
import android.content.res.Resources;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.R;

/**
 * Data object for holding information about a conversation.
 */
public class Conversation {

    public Contact contact;
    public boolean pinned;
    public boolean read;
    public long timestamp;
    public String snippet;

    public Conversation(String name, int color, boolean pinned, boolean read, long timestamp,
                        String snippet) {
        this(new Contact(name, color), pinned, read, timestamp, snippet);
    }

    public Conversation(Contact contact, boolean pinned, boolean read, long timestamp,
                        String snippet) {
        this.contact = contact;
        this.pinned = pinned;
        this.read = read;
        this.timestamp = timestamp;
        this.snippet = snippet;
    }

    public static List<Conversation> getFakeConversations(Resources resources) {
        List<Conversation> conversations = new ArrayList<>();

        conversations.add(new Conversation(
                "Luke Klinker",
                resources.getColor(R.color.materialIndigo),
                true,
                true,
                System.currentTimeMillis() - (1000 * 60 * 60),
                "So maybe not going to be able to get platinum huh?"
        ));

        conversations.add(new Conversation(
                "Matt Swiontek",
                resources.getColor(R.color.materialRed),
                true,
                true,
                System.currentTimeMillis() - (1000 * 60 * 60 * 12),
                "Whoops ya idk what happened but anysho drive safe"
        ));

        conversations.add(new Conversation(
                "Kris Klinker",
                resources.getColor(R.color.materialPink),
                false,
                false,
                System.currentTimeMillis() - (1000 * 60 * 20),
                "Will probably be there from 6:30-9, just stop by when you can!"
        ));

        conversations.add(new Conversation(
                "Andrew Klinker",
                resources.getColor(R.color.materialBlue),
                false,
                true,
                System.currentTimeMillis() - (1000 * 60 * 60 * 26),
                "Just finished, it was a lot of fun"
        ));

        conversations.add(new Conversation(
                "Aaron Klinker",
                resources.getColor(R.color.materialGreen),
                false,
                true,
                System.currentTimeMillis() - (1000 * 60 * 60 * 32),
                "Yeah I'll do it when I get home"
        ));

        conversations.add(new Conversation(
                "Mike Klinker",
                resources.getColor(R.color.materialBrown),
                false,
                true,
                System.currentTimeMillis() - (1000 * 60 * 60 * 55),
                "Yeah so hiking around in some place called beaver meadows now."
        ));

        conversations.add(new Conversation(
                "Ben Madden",
                resources.getColor(R.color.materialPurple),
                false,
                true,
                System.currentTimeMillis() - (1000 * 60 * 60 * 78),
                "Maybe they'll run into each other on the way back... idk"
        ));

        return conversations;
    }

}