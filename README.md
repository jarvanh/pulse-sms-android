![feature graphic](/artwork/repo-header.png)

# Pulse SMS

The goal of this project is to create an SMS/MMS app that has full support for all of the features
that users love, is based on material design, and supports a strong end-to-end encryption version of a
tablet/desktop messenger that sends messages through your personal phone number.

Check out Pulse on the [Play Store](https://play.google.com/store/apps/details?id=xyz.klinker.messenger)
or the app's [website](https://messenger.klinkerapps.com/overview) for a more comprehensive overview
of the app's features, platforms, and functionality!

Many other Pulse platforms are also open source (all but the backend). If you would like to take a
look at them, you can find them on Klinker Apps [GitHub](https://github.com/klinker-apps?q=pulse).

## Compiling Pulse

This repo is **almost** ready to go, right out of the box. There are just two properties files that you need
to create for the build process to succeed: `api_keys.properties` and `keystore.properties`.

#### Set up API keys

You'll need to set up a few different API keys. Rename the `api_keys.properties.example`
file to `api_keys.properties`. This alone will get the build working and might be perfectly fine for your usage. 

If you are using a self-built version of the app on a daily basis, then you might want to put in a few of your own API keys, rather than the public ones I have available. Please see the notes at the top of the file to learn more.

#### Set up release keystore

Whether you are going to make a release build of the app or not, you will need to copy the `keystore.properties.example`
file to `keystore.properties`. If you aren't going to make a release build for anything, just leave it as is.

If you are going to make a release build, you will need to add your keystore to the repo and fill in
fields outlined by that file.

## Contributing to Pulse

Contributions are welcome!

* If you just want to report a bug or file a feature request, I have a [centralized issues repo](https://github.com/klinker-apps/pulse-sms-issues/issues) for tracking issues/requests across all of Pulse's platforms. Please file the issue there.
* Any other contributions can just go through the [Pull Requests](https://github.com/klinker-apps/pulse-sms-android/pulls) on this repo.

If you are looking to make a large change, it is probably best to discuss it with me first. Open up an [issue](https://github.com/klinker-apps/pulse-sms-issues/issues/new?template=contribution_question.md), letting me know that this is something that you would like to make a PR for, and I will tell you if it is something I would consider integrating into the app. Even if it ends up being something I do not think would work in a wide-release to all of my users, it is still something that you can add and use yourself!

## License

    Copyright (C) 2020 Luke Klinker

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
