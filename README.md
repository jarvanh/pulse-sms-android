![feature graphic](artwork/play\ store/feature\ graphic\ white.png)

# Messenger

The goal of this project is to create an SMS/MMS app that has full support for all of the features
that users love, is based on material design, and supports a strong-encryption version of a tablet
messenger that sends messages through your phone.

The project will be separate from my previous messaging experience with Sliding Messaging and
EvolveSMS and will be completely rewritten from scratch for performance and consistency. The
designs themselves are loosely based off of Google Inbox and adapted as a messenger instead of
email client.

### APIs

The project will include Java APIs that allow any SMS app to use the online service and hook up to the
tablet app. This will be important for EvolveSMS so that support can be there and our userbase can
be larger. This app will also be very different from EvolveSMS, so the users can have their
preference on which they like more. This Java project will be part of this repo and the APIs will be
accessed using [Retrofit](http://square.github.io/retrofit/). 

### Server

The server will be built so that I can host it on AWS myself and provide the service to users.

As a note, all messages and information will be stored in encrypted form at all points in the
process, except on the user's device. Strong end-to-end encryption will be rigorously enforced
and documented.

You can find this repo at [messenger-server](https://github.com/klinker41/messenger-server).

### Building

To build, you'll need to set up a few different API keys. Rename the `api_keys.properties.example`
file to `api_keys.properties`. This file already contains the public Giphy beta testing key as an
example for you. You should change this to your own key that you acquire from Giphy when publishing
to production.

After that a simple `./gradlew clean build` will assemble everything you need for the project.

## License

    Copyright (C) 2016 Jake Klinker

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
