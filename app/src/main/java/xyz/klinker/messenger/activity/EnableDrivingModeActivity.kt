package xyz.klinker.messenger.activity

import android.app.Activity
import android.os.Bundle
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.Settings

// Added by request. Someone wanted to start an activity from tasker to turn this on and off.
class EnableDrivingModeActivity : Activity() {

    companion object {
        private const val ENABLE_DRIVING_MODE = "extra_enable_driving_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)

        val enabled = intent.getBooleanExtra(ENABLE_DRIVING_MODE, true)
        Settings.setValue(this, getString(R.string.pref_driving_mode), enabled)

        finish()
        overridePendingTransition(0, 0)
    }

}