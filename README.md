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

This repo is **almost** ready to go, right out of the box. There are just two properties files that
you need to create for the build process to succeed: `api_keys.properties` and `keystore.properties`. 
Both files can simply be copy and paste from the examples, if you choose.

#### Set up API keys

You'll need to set up a few different API keys. Rename the `api_keys.properties.example`
file to `api_keys.properties`. This alone will get the build working and might be perfectly fine 
for your usage. 

If you are using a self-built version of the app on a daily basis, then you might want to put in a 
few of your own API keys, rather than the public ones I have available. Please see the notes at the 
top of the file to learn more.

#### Set up release keystore

Whether you are going to make a release build of the app or not, you will need to copy the 
`keystore.properties.example` file to `keystore.properties`. If you aren't going to make a release 
build for anything, just leave it as is.

If you are going to make a release build, you will need to add your keystore to the repo and fill in
fields outlined by that file.

### Running the Tests

Pulse contains unit and integration tests. To run all of them, run:

```
$ ./gradlew testDebugUnitTest
```

Pulse uses Robolectric for Android related tests. It does not contain UI-espresso tests.

## Contributing to Pulse

Contributions are welcome!

* If you just want to report a bug or file a feature request, I have a 
[centralized issues repo](https://github.com/klinker-apps/pulse-sms-issues/issues) for tracking 
issues/requests across all of Pulse's platforms. Please file the issue there.
* Any other contributions can just go through the 
[Pull Requests](https://github.com/klinker-apps/pulse-sms-android/pulls) on this repo.

If you are looking to make a large change, it is probably best to discuss it with me first. Open up 
an [issue](https://github.com/klinker-apps/pulse-sms-issues/issues/new?template=contribution_question.md), 
letting me know that this is something that you would like to make a PR for, and I can tell you what
I think. 

### Open Source Experiments Settings Page

As a consumer app, allowing unlimited customization and endless settings pages is not Pulse's goal.
While some may disagree, ultimately this does not add up to the best user experience. More settings 
means a much higher overhead for new users, as well as a more difficult time for existing users. 
I have done my best to enforce this vision by provide logical defaults, throughout the app. While Pulse
is not light on customization, there is a balance between what could be considered "too much".

This vision is somewhat different in the eyes of an open source project, however. A major benefit of
open source software is that you can customize it however you want. With that in mind, even if a 
feature you are suggesting is not something that I want to officially support in the app, that does not mean
it can't and shouldn't be included! If you find use out of it, chances are someone else will, as well.

Within Pulse, I have made an "Open Source Experiments" settings page. There are disclaimers at the top
that these preferences come as contributions from the community, without official support. This would 
be a great place to put any "tweak" options that you wish to include.

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
