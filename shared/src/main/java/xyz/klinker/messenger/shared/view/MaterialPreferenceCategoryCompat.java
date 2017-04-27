package xyz.klinker.messenger.shared.view;

import android.content.Context;
import android.support.v7.preference.PreferenceCategory;
import android.util.AttributeSet;

import xyz.klinker.messenger.shared.R;

public class MaterialPreferenceCategoryCompat extends PreferenceCategory {

    public MaterialPreferenceCategoryCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public MaterialPreferenceCategoryCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaterialPreferenceCategoryCompat(Context context) {
        super(context);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_category_card);
    }
}
