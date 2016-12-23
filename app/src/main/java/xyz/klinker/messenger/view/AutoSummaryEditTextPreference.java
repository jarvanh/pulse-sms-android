package xyz.klinker.messenger.view;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class AutoSummaryEditTextPreference extends EditTextPreference {
    public AutoSummaryEditTextPreference(Context context) {
        this(context, null);
    }

    public AutoSummaryEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            setSummary(getSummary());
        }
    }

    @Override
    public CharSequence getSummary() {
        return getText();
    }
}
