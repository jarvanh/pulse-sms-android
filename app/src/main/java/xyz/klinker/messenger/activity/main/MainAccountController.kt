package xyz.klinker.messenger.activity.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.google.android.material.snackbar.Snackbar
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.InitialLoadActivity
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.activity.OnboardingActivity
import xyz.klinker.messenger.api.implementation.LoginActivity
import xyz.klinker.messenger.shared.MessengerActivityExtras
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.service.ApiDownloadService
import xyz.klinker.messenger.shared.service.FirebaseTokenUpdateCheckService
import xyz.klinker.messenger.shared.service.NewMessagesCheckService

class MainAccountController(private val activity: MessengerActivity) {

    private var startImportOrLoad = false

    private var downloadReceiver: BroadcastReceiver? = null
    private val refreshAllReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            activity.recreate()
        }
    }

    fun startIntroOrLogin(savedInstanceState: Bundle?) {
        if (Settings.firstStart && savedInstanceState == null) {
            if (FeatureFlags.SKIP_INTRO_PAGER) {
                startLoad(MessengerActivityExtras.REQUEST_ONBOARDING)
            } else {
                val hasTelephone = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                val hasPhoneFeature = hasTelephone && !activity.resources.getBoolean(R.bool.is_tablet)
                if (hasPhoneFeature) {
                    activity.startActivityForResult(
                            Intent(activity, OnboardingActivity::class.java),
                            MessengerActivityExtras.REQUEST_ONBOARDING
                    )
                } else {
                    // if it isn't a phone, then we want to go straight to the login
                    // and skip the onboarding, since they would have done it on their phone
                    val login = Intent(activity, InitialLoadActivity::class.java)
                    activity.startActivity(login)
                    activity.finish()
                }
            }

            startImportOrLoad = true
        }
    }

    // if it isn't a phone, then we want to force the login.
    // if it is a phone, they can choose to log in when they want to
    fun startLoad(requestCode: Int) {
        if (requestCode == MessengerActivityExtras.REQUEST_ONBOARDING) {
            val hasTelephone = activity.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
            val login = Intent(activity, InitialLoadActivity::class.java)
            login.putExtra(LoginActivity.ARG_SKIP_LOGIN, hasTelephone)

            activity.startActivity(login)
            activity.finish()
        }
    }

    fun listenForFullRefreshes() {
        activity.registerReceiver(refreshAllReceiver,
                IntentFilter(NewMessagesCheckService.REFRESH_WHOLE_CONVERSATION_LIST))
    }

    fun stopListeningForRefreshes() {
        try {
            activity.unregisterReceiver(refreshAllReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshAccountToken() {
        if (!startImportOrLoad) {
            try {
                activity.startService(Intent(activity, FirebaseTokenUpdateCheckService::class.java))
            } catch (e: IllegalStateException) {
                // started from background. Not sure how that would be, but it will retry the next time the app is opened
            }

            Handler().postDelayed({
                NewMessagesCheckService.startService(activity)
            }, 3000)
        } else {
            startImportOrLoad = false
        }
    }

    fun startResyncingAccount() {
        Handler().postDelayed({
            downloadReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    activity.navController.drawerItemClicked(R.id.drawer_conversation)
                }
            }

            activity.registerReceiver(downloadReceiver,
                    IntentFilter(ApiDownloadService.ACTION_DOWNLOAD_FINISHED))

            ApiDownloadService.start(activity)
            val snackbar = Snackbar.make(activity.snackbarContainer,
                    activity.getString(R.string.downloading_and_decrypting),
                    Snackbar.LENGTH_LONG)
            activity.insetController.adjustSnackbar(snackbar)
            snackbar.show()
        }, 1000)
    }

    fun stopListeningForDownloads() {
        if (downloadReceiver != null) {
            activity.unregisterReceiver(downloadReceiver)
        }
    }
}