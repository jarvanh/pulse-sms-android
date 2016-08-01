![feature graphic](https://lh3.googleusercontent.com/9FI-TLi1q6Xis9SP1vubtZBDwL0nISRtwZ3WsrbmyshTiILg4ARI34UNBjnpyAh4sT1s0rdOzXUbcrhETTnj5Xz5HwXiahwgQvt9YprNIhwxhHQPK5SYHqXLObk87HRl-qsX6j6SSLyjqyIsYGPTPOlsX2zPttsj8LVBTKA8Y9082QLr72GbpGeyD0DMV0WerwDOIxY3K5fSjzI5OWlQaSLR1ZKSZ5wjT75lTMmYiXO9jttjobzK6V-dYHULD_vvfJwVKLqvZ-723AdO2dZHG9WfeFCsOo13o6d2p0pCHbCsjjKBEXUa5oNcwbdSQePl5eQPmiTMh1kOgbRr6DyhqlbCOJKZDSjh50Yn4dFClzY2Zuig_84YcgvK_hZ4r41Z-afrbJp06lavWgV-_GoKBq-xnY0lyOyYnY_-IKR9dRk_Qa4lKMedCWyxtH_glmtZc4TZU5KmtlyWB0cpHUoSI5WFu1Akk14hknZlO_4gF-sRqOTRKVm2xH9yb1WpmgpoWH4AAcA6BlOxftvBGhJ3Mt6y6jMp3mnzEMY9Wpu-GdZY39yxJa1-xEvzAPEYj0TOPM_OtS7xVTiC3kIUYk__ZaBMceLTYxTM=w930-h454-no)

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
