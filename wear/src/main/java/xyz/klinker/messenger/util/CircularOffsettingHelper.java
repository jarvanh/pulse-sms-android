package xyz.klinker.messenger.util;

import android.support.wearable.view.DefaultOffsettingHelper;
import android.support.wearable.view.WearableRecyclerView;
import android.view.View;

public class CircularOffsettingHelper extends DefaultOffsettingHelper {
    @Override
    public void updateChild(View child, WearableRecyclerView parent) {
        int progress = child.getTop() / parent.getHeight();
        child.setTranslationX(-child.getHeight() * progress);
    }
}
