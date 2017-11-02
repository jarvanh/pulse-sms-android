package xyz.klinker.messenger.shared.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.support.annotation.RequiresApi
import xyz.klinker.messenger.shared.data.FeatureFlags

import xyz.klinker.messenger.shared.util.ActivityUtils

@RequiresApi(api = Build.VERSION_CODES.N)
class ComposeTileService : TileService() {

    override fun onClick() {
        try {
            val compose = ActivityUtils.buildForComponent(if (FeatureFlags.QUICK_COMPOSE)
                ActivityUtils.QUICK_SHARE_ACTIVITY else ActivityUtils.COMPOSE_ACTIVITY)
            startActivityAndCollapse(compose)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartListening() {
        try {
            qsTile.state = Tile.STATE_ACTIVE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
