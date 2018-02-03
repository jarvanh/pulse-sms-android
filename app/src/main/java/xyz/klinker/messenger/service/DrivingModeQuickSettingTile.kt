package xyz.klinker.messenger.service

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import xyz.klinker.messenger.R
import xyz.klinker.messenger.shared.data.Settings

@TargetApi(Build.VERSION_CODES.N)
class DrivingModeQuickSettingTile : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        setState()
    }

    override fun onStartListening() {
        super.onStartListening()
        setState()
    }

    override fun onClick() {
        Settings.setValue(this, getString(R.string.pref_driving_mode), !Settings.drivingMode)
        DrivingModeQuickSettingTile.updateState(this)
    }

    private fun setState() {
        if (qsTile != null) {
            if (Settings.drivingMode) {
                qsTile.state = Tile.STATE_ACTIVE
            } else {
                qsTile.state = Tile.STATE_INACTIVE
            }

            qsTile.updateTile()
        }
    }

    companion object {
        fun updateState(context: Context) {
            TileService.requestListeningState(context,
                    ComponentName(context, DrivingModeQuickSettingTile::class.java))
        }
    }
}