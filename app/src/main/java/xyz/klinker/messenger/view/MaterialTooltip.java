package xyz.klinker.messenger.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.telecom.Call;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.data.Settings;
import xyz.klinker.messenger.util.DensityUtil;

@SuppressLint("ViewConstructor")
public class MaterialTooltip extends FrameLayout {

    private static final int ANIMATION_TIME = 300;
    private static final Interpolator INTERPOLATOR = new DecelerateInterpolator();

    private View content;
    private TextView text;
    private Button gotIt;
    private ViewGroup.LayoutParams contentParams;

    private View dim;

    private int distanceFromTop;
    private int distanceFromStart;
    private int screenWidth;
    private int screenHeight;
    private ViewGroup androidContentView = null;

    private Callback callback;

    public MaterialTooltip(Activity context, Options options) {
        super(context);
        init(context, LayoutInflater.from(context).inflate(R.layout.tooltip, this, false), options);
    }

    private void init(Activity context, @NonNull View content, Options options) {

        // get the main content view of the display
        androidContentView = (FrameLayout) context.findViewById(android.R.id.content).getRootView();

        // initialize the display size
        Display display = context.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        screenHeight = size.y;
        screenWidth = size.x;

        // set up the content we want to show
        this.content = content;
        this.text = (TextView) content.findViewById(R.id.tip_text);
        this.gotIt = (Button) content.findViewById(R.id.got_it_button);
        contentParams = content.getLayoutParams();

        setAlpha(0f);

        setWidth(DensityUtil.toPx(context, options.width));
        setDistanceFromStart(DensityUtil.toPx(context, options.startOffset));
        setDistanceFromTop(DensityUtil.toPx(context, options.topOffset));

        gotIt.setTextColor(Color.WHITE);
        text.setText(options.text);

        gotIt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();

                if (callback != null) {
                    callback.onGotIt();
                }
            }
        });

        // add the background dim to the frame
        dim = new View(context);
        dim.setBackgroundColor(Color.BLACK);
        dim.setAlpha(.6f);

        FrameLayout.LayoutParams dimParams =
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dim.setLayoutParams(dimParams);

        // add the dim and the content view to the upper level frame layout
        //addView(dim);
        addView(content);
    }

    /**
     * Sets how far away from the top of the screen the view should be displayed.
     * Distance should be the value in PX.
     *
     * @param distance the distance from the top in px.
     */
    private void setDistanceFromTop(int distance) {
        this.distanceFromTop = distance;
    }

    /**
     * Sets how far away from the left side of the screen the view should be displayed.
     * Distance should be the value in PX.
     *
     * @param distance the distance from the left in px.
     */
    private void setDistanceFromStart(int distance) {
        this.distanceFromStart = distance;
    }

    /**
     * Sets the width of the view in PX.
     *
     * @param width the width of the circle in px
     */
    private void setWidth(int width) {
        contentParams.width = width;
        content.setLayoutParams(contentParams);
    }

    /**
     * Sets the height of the view in PX.
     *
     * @param height the height of the circle in px
     */
    private void setHeight(int height) {
        contentParams.height = height;
        content.setLayoutParams(contentParams);
    }

    /**
     * Sets the width of the window according to the screen width.
     *
     * @param percent of screen width
     */
    public void setWidthByPercent(@FloatRange(from=0,to=1) float percent) {
        setWidth((int) (screenWidth * percent));
    }

    /**
     * Sets the height of the window according to the screen height.
     *
     * @param percent of screen height
     */
    public void setHeightByPercent(@FloatRange(from=0,to=1) float percent) {
        setHeight((int) (screenHeight * percent));
    }

    /**
     * Show the content of the PeekView by adding it to the android.R.id.content FrameLayout.
     */
    public void show(Callback callback) {
        this.callback = callback;
        isShowing = true;

        androidContentView.addView(this);

        // set the translations for the content view
        content.setTranslationX(distanceFromStart);
        content.setTranslationY(distanceFromTop);

        // animate the alpha of the PeekView
        // animate with a fade
        animate().withLayer()
                .alpha(1f).setDuration(ANIMATION_TIME).setInterpolator(INTERPOLATOR)
                .setListener(null);
    }

    /**
     * Hide the PeekView and remove it from the android.R.id.content FrameLayout.
     */
    public void hide() {
        isShowing = false;

        // animate with a fade
        animate().withLayer()
                .alpha(0f).setDuration(ANIMATION_TIME).setInterpolator(INTERPOLATOR)
                .setListener(null);
    }

    private boolean isShowing = false;
    public boolean isShowing() {
        return isShowing;
    }

    public static class Options {

        public String text;
        public int topOffset;
        public int startOffset;
        public int width;
        public int doneButtonTextColor;

        public Options(int topOffsetDp, int startOffsetDp, int widthDp, int doneButtonTextColor) {
            this.topOffset = topOffsetDp;
            this.startOffset = startOffsetDp;
            this.width = widthDp;
            this.doneButtonTextColor = doneButtonTextColor;
        }

        public Options setText(String text) {
            this.text = text;
            return this;
        }
    }

    public interface Callback {
        void onGotIt();
    }

}