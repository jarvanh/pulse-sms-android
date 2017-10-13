package xyz.klinker.messenger.shared.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import xyz.klinker.messenger.shared.R;
import xyz.klinker.messenger.shared.util.ColorUtils;

public class WhitableToolbar extends Toolbar {

    private int backgroundColor = Integer.MIN_VALUE;

    public WhitableToolbar(Context context) {
        super(context);
    }

    public WhitableToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WhitableToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
        this.backgroundColor = color;

        int textColor = getTextColor();
        ColorStateList tintList = ColorStateList.valueOf(textColor);

        setTitleTextColor(textColor);
        if (getOverflowIcon() != null) getOverflowIcon().setTintList(tintList);
        if (getNavigationIcon() != null) getNavigationIcon().setTintList(tintList);
    }

    @Override
    public void setNavigationIcon(int res) {
        super.setNavigationIcon(res);

        if (getNavigationIcon() != null) {
            getNavigationIcon().setTintList(ColorStateList.valueOf(getTextColor()));
        }
    }

    @Override
    public void inflateMenu(int menu) {
        super.inflateMenu(menu);

        for (int i = 0; i < getMenu().size(); i++) {
            if (getMenu().getItem(i).getIcon() != null) {
                getMenu().getItem(i).getIcon().setTintList(ColorStateList.valueOf(getTextColor()));
            }
        }
    }

    public int getTextColor() {
        if (backgroundColor == Integer.MIN_VALUE || ColorUtils.INSTANCE.isColorDark(backgroundColor)) {
            return Color.WHITE;
        } else {
            return getResources().getColor(R.color.lightToolbarTextColor);
        }
    }
}
