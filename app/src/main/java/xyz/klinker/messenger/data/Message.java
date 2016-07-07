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

import java.util.ArrayList;
import java.util.List;

/**
 * Holds information regarding messages (eg what type they are, what they contain and a timestamp).
 */
public class Message {

    public static final int TYPE_RECEIVED = 0;
    public static final int TYPE_SENT = 1;
    public static final int TYPE_SENDING = 2;
    public static final int TYPE_ERROR = 3;

    public int type;
    public String text;
    public long timestamp;

    public Message(int type, String text, long timestamp) {
        this.type = type;
        this.text = text;
        this.timestamp = timestamp;
    }

    public static List<Message> getFakeMessages() {
        List<Message> messages = new ArrayList<>();

        messages.add(new Message(
                TYPE_RECEIVED,
                "Do you want to go to summerfest this weekend?",
                System.currentTimeMillis() - (1000 * 60 * 60 * 12) - (1000 * 60 * 30)
        ));

        messages.add(new Message(
                TYPE_SENT,
                "Yeah, I'll probably go on Friday.",
                System.currentTimeMillis() - (1000 * 60 * 60 * 12)
        ));

        messages.add(new Message(
                TYPE_SENT,
                "I started working on the designs for a new messaging app today... I'm thinking " +
                        "that it could be somewhere along the lines of a compliment to Evolve. " +
                        "The main app will be focused on tablet design and so Evolve could " +
                        "support hooking up to the same backend and the two could be used " +
                        "together. Or, users could just use this app on their phone as well... " +
                        "up to them which they prefer.",
                System.currentTimeMillis() - (1000 * 60 * 60 * 8) - (1000 * 60 * 6)
        ));

        messages.add(new Message(
                TYPE_RECEIVED,
                "Are you going to make this into an actual app?",
                System.currentTimeMillis() - (1000 * 60 * 60 * 8)
        ));

        messages.add(new Message(
                TYPE_SENT,
                "dunno",
                System.currentTimeMillis() - (1000 * 60 * 60 * 7) - (1000 * 60 * 55)
        ));

        messages.add(new Message(
                TYPE_SENT,
                "I got to build some Legos, plus get 5 extra character packs and 3 level packs " +
                        "with the deluxe edition lol",
                System.currentTimeMillis() - (1000 * 60 * 38)
        ));

        messages.add(new Message(
                TYPE_RECEIVED,
                "woah nice one haha",
                System.currentTimeMillis() - (1000 * 60 * 37)
        ));

        messages.add(new Message(
                TYPE_SENT,
                "Already shaping up to be a better deal than battlefront!",
                System.currentTimeMillis() - (1000 * 60 * 23)
        ));

        messages.add(new Message(
                TYPE_RECEIVED,
                "is it fun?",
                System.currentTimeMillis() - (1000 * 60 * 22)
        ));

        messages.add(new Message(
                TYPE_SENT,
                "So far! Looks like a lot of content in the game too. Based on the trophies " +
                        "required at least",
                System.currentTimeMillis() - (1000 * 60 * 20)
        ));

        messages.add(new Message(
                TYPE_RECEIVED,
                "so maybe not going to be able to get platinum huh? haha",
                System.currentTimeMillis() - (1000 * 60 * 16)
        ));

        messages.add(new Message(
                TYPE_SENT,
                "Oh, I will definitely get it! Just might take 24+ hours to do it... and when " +
                        "those 24 hours are in a single week, things get to be a little tedious. " +
                        "Hopefully I don't absolutely hate the game once I finish!",
                System.currentTimeMillis() - (1000 * 60)
        ));

        return messages;
    }

}
