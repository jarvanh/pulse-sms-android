package xyz.klinker.messenger.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.VisibleForTesting;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
public class DualSimUtils {

    private static volatile DualSimUtils dualSimUtils;

    /**
     * Gets a new instance (singleton) of Dual SIM utils.
     *
     * @param context the current application context.
     * @return the utils instance.
     */
    public static synchronized DualSimUtils get(Context context) {
        if (dualSimUtils == null) {
            dualSimUtils = new DualSimUtils(context);
        }

        return dualSimUtils;
    }

    protected DualSimUtils() {
        throw new RuntimeException("Don't initialize this!");
    }

    private DualSimUtils(final Context context) {
        init(context);
    }

    private Context context;
    private List<SubscriptionInfo> availableSims;

    private void init(Context context) {
        this.context = context;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager manager = SubscriptionManager.from(context);
            this.availableSims = manager.getActiveSubscriptionInfoList();
        } else {
            this.availableSims = new ArrayList<>();
        }
    }

    public List<SubscriptionInfo> getAvailableSims() {
        return availableSims;
    }

    public String getNumberFromSimSlot(int simSlot) {
        for (SubscriptionInfo sim : availableSims) {
            if (sim.getSimSlotIndex() == simSlot) {
                return sim.getNumber();
            }
        }

        return null;
    }

    public int getSimSlotForPhoneNumber(String phoneNumber) {

        for (SubscriptionInfo sim : availableSims) {
            if (sim.getNumber().equals(phoneNumber)) {
                return sim.getSimSlotIndex();
            }
        }

        return -1;
    }

    public void updateMessageWithSimSlot() {

    }
}
