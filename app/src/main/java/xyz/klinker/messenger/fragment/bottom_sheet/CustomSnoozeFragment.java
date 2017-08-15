package xyz.klinker.messenger.fragment.bottom_sheet;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import xyz.klinker.messenger.R;
import xyz.klinker.messenger.activity.MessengerActivity;
import xyz.klinker.messenger.api.implementation.Account;
import xyz.klinker.messenger.api.implementation.ApiUtils;
import xyz.klinker.messenger.shared.data.Settings;
import xyz.klinker.messenger.shared.util.TimeUtils;

public class CustomSnoozeFragment extends TabletOptimizedBottomSheetDialogFragment {

    private EditText minutes;
    private EditText hours;

    @Override
    protected View createLayout(LayoutInflater inflater) {
        final View contentView = inflater.inflate(R.layout.bottom_sheet_custom_snooze, null, false);

        minutes = (EditText) contentView.findViewById(R.id.minutes);
        hours = (EditText) contentView.findViewById(R.id.hours);
        contentView.findViewById(R.id.snooze).setOnClickListener(view -> snooze());

        hours.setSelection(hours.getText().length());

        return contentView;
    }

    private void snooze() {
        long snoozeTil = System.currentTimeMillis() + getMinutesTime() + getHoursTime();

        Settings.get(getActivity()).setValue(getActivity(), getString(R.string.pref_snooze), snoozeTil);
        new ApiUtils().updateSnooze(Account.get(getActivity()).accountId, snoozeTil);

        dismiss();

        if (getActivity() instanceof MessengerActivity) {
            ((MessengerActivity) getActivity()).snoozeIcon();
        }
    }

    private long getMinutesTime() {
        return readNumberSafely(minutes) * TimeUtils.MINUTE;
    }

    private long getHoursTime() {
        return readNumberSafely(hours) * TimeUtils.HOUR;
    }

    private int readNumberSafely(EditText et) {
        try {
            return Integer.parseInt(et.getText().toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
