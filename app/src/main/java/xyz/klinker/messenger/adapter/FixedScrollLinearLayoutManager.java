package xyz.klinker.messenger.adapter;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

public class FixedScrollLinearLayoutManager extends LinearLayoutManager {

    private boolean canScroll = true;

    public FixedScrollLinearLayoutManager(Context context) {
        super(context);
    }

    public void setCanScroll(boolean canScroll) {
        this.canScroll = canScroll;
    }

    @Override
    public boolean canScrollVertically() {
        return canScroll && super.canScrollVertically();
    }
}
