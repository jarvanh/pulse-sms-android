package xyz.klinker.messenger.shared.service;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;

import xyz.klinker.messenger.shared.util.ActivityUtils;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ComposeTileService extends TileService {

    @Override
    public void onClick() {
        Intent compose = ActivityUtils.buildForComponent(ActivityUtils.COMPOSE_ACTIVITY);
        startActivityAndCollapse(compose);
    }

    @Override
    public void onStartListening() {
        try {
            getQsTile().setState(Tile.STATE_ACTIVE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
