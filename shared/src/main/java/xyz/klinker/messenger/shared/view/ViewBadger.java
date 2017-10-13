package xyz.klinker.messenger.shared.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.util.DensityUtil;

@SuppressLint({"AppCompatCustomView", "ViewConstructor"})
public class ViewBadger extends TextView {

    private static final int CORNER_RADIUS = 2;
    private static final int MARGIN_LEFT = 14;
    private static final int MARGIN_TOP = 14;

    private Context context;

    public ViewBadger(Context context, View target) {
        super(context, null, android.R.attr.textViewStyle);
        init(context, target);
    }

    private void init(Context context, View target) {
        this.context = context;

        int paddingPixels = DensityUtil.INSTANCE.toDp(context, 1);

        setTextSize(10);
        setTypeface(Typeface.DEFAULT_BOLD);
        setPadding(paddingPixels, 0, paddingPixels, 0);

        applyTo(target);
        show();
    }

    private void applyTo(View target) {
        LayoutParams lp = target.getLayoutParams();
        ViewParent parent = target.getParent();
        FrameLayout container = new FrameLayout(context);

        ViewGroup group = (ViewGroup) parent;
        int index = group.indexOfChild(target);

        group.removeView(target);
        group.addView(container, index, lp);

        container.addView(target);
        container.addView(this);
        group.invalidate();
    }

    private void show() {
        if (getBackground() == null) {
            setBackgroundDrawable(getDefaultBackground());
            setTextColor(getContext().getResources().getColor(R.color.background));
        }

        applyLayoutParams();

        this.setVisibility(View.VISIBLE);
    }

    private ShapeDrawable getDefaultBackground() {
        int r = DensityUtil.INSTANCE.toDp(context, CORNER_RADIUS);
        float[] outerR = new float[] {r, r, r, r, r, r, r, r};

        RoundRectShape rr = new RoundRectShape(outerR, null, null);
        ShapeDrawable drawable = new ShapeDrawable(rr);
        drawable.getPaint().setColor(getContext().getResources().getColor(R.color.secondaryText));

        return drawable;
    }

    private void applyLayoutParams() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.START | Gravity.TOP;
        lp.setMargins(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, 0);

        setLayoutParams(lp);
    }
}
