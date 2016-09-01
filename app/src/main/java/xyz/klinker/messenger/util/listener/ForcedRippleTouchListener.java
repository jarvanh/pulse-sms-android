package xyz.klinker.messenger.util.listener;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.MotionEvent;
import android.view.View;

public class ForcedRippleTouchListener implements View.OnTouchListener {

    private static final long RIPPLE_TIMEOUT_MS = 50;

    private View rippleView;

    public ForcedRippleTouchListener(View rippleView) {
        this.rippleView = rippleView;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            forceRippleAnimation(rippleView, event);
        }

        return false;
    }

    private void forceRippleAnimation(View view, MotionEvent event) {
        Drawable background = ((View) view.getParent()).getBackground();

        if (background instanceof RippleDrawable) {
            final RippleDrawable rippleDrawable = (RippleDrawable) background;

            rippleDrawable.setState(new int[]{android.R.attr.state_pressed,
                    android.R.attr.state_enabled});
            rippleDrawable.setHotspot(event.getX(), event.getY());

            view.postDelayed(new Runnable() {
                @Override
                public void run() {
                    rippleDrawable.setState(new int[]{});
                }
            }, RIPPLE_TIMEOUT_MS);
        }
    }
}
