package xyz.klinker.messenger.service;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;

import xyz.klinker.messenger.activity.ComposeActivity;

@RequiresApi(api = Build.VERSION_CODES.N)
public class ComposeTileService extends TileService {

    @Override
    public void onClick() {
        startActivityAndCollapse(
                new Intent(this, ComposeActivity.class));
    }

    @Override
    public void onStartListening() {
        getQsTile().setState(Tile.STATE_ACTIVE);
    }
}
