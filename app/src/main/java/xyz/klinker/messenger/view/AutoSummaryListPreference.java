package xyz.klinker.messenger.view;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class AutoSummaryListPreference extends ListPreference {

    public AutoSummaryListPreference(Context context) {
        this(context, null);
    }

    public AutoSummaryListPreference(Context context, AttributeSet attrs) {
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
        int pos = findIndexOfValue(getValue());
        return getEntries()[pos];
    }
}