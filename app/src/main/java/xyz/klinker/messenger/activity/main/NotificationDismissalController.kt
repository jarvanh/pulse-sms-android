package xyz.klinker.messenger.activity.main

import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.shared.MessengerActivityExtras

class NotificationDismissalController(private val activity: MessengerActivity) {

    private val intent
        get() = activity.intent
}