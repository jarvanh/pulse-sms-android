package xyz.klinker.messenger.service

import android.annotation.TargetApi
import android.content.*
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.Settings

@TargetApi(Build.VERSION_CODES.N)
class DrivingModeQuickSettingTile : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        setState()

        Log.v(LOG_TAG, "tile added")
    }

    override fun onStartListening() {
        super.onStartListening()
        setState()

        Log.v(LOG_TAG, "start listening")
    }

    override fun onClick() {
        Settings.setValue(this, getString(R.string.pref_driving_mode), !Settings.drivingMode)
        setState()
    }

    private fun setState() {
        if (Settings.drivingMode) {
            qsTile?.state = Tile.STATE_ACTIVE
        } else {
            qsTile?.state = Tile.STATE_INACTIVE
        }

        qsTile?.updateTile()
    }

    companion object {
        private const val LOG_TAG = "pulse_driving_mode"
        private const val UPDATE_STATE_BROADCAST = "xyz.klinker.messenger.UPDATE_DRIVING_TILE"
        fun updateState(context: Context) {
//            context.sendBroadcast(Intent(UPDATE_STATE_BROADCAST))
        }
    }
}