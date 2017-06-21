package xyz.klinker.messenger.shared.view;

import android.content.Context;
import android.support.v14.preference.SwitchPreference;
import android.util.AttributeSet;

import xyz.klinker.messenger.shared.R;

public class AlwaysOffSwitchPreference extends SwitchPreference {

    public AlwaysOffSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AlwaysOffSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AlwaysOffSwitchPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setChecked(false);
    }
}
