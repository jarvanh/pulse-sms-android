package xyz.klinker.messenger.activity.main

import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.util.PermissionsUtils

class MainPermissionHelper(private val activity: MessengerActivity) {

    fun requestPermissions() {
        if (PermissionsUtils.checkRequestMainPermissions(activity)) {
            PermissionsUtils.startMainPermissionRequest(activity)
        }
    }

    fun requestDefaultSmsApp() {
        if (Account.primary && !PermissionsUtils.isDefaultSmsApp(activity)) {
            PermissionsUtils.setDefaultSmsApp(activity)
        }
    }

    fun handlePermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        try {
            PermissionsUtils.processPermissionRequest(activity, requestCode, permissions, grantResults)
            if (requestCode == MessengerActivityExtras.REQUEST_CALL_PERMISSION) {
                activity.navController.messageActionDelegate.callContact()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}