package xyz.klinker.messenger.shared.util

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager

@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
object DualSimUtils {

    val availableSims = mutableListOf<SubscriptionInfo>()
    private var manager: SubscriptionManager? = null

    val defaultPhoneNumber: String?
        get() {
            try {
                if (manager != null && manager!!.activeSubscriptionInfoCount > 0) {
                    return manager!!.activeSubscriptionInfoList[0].number
                }
            } catch (e: SecurityException) {
            }

            return null
        }

    fun init(context: Context) {
        if (canHandleDualSim()) {
            manager = SubscriptionManager.from(context)

            try {
                this.availableSims.clear()
                this.availableSims.addAll(manager!!.activeSubscriptionInfoList)
                if (availableSims.size <= 1) {
                    // not a dual sim phone, ignore this.
                    this.availableSims.clear()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                this.availableSims.clear()
            }
        }
    }

    fun getSubscriptionInfo(simSubscription: Int?): SubscriptionInfo? {
        if (simSubscription == null || simSubscription == 0 || simSubscription == -1 || !canHandleDualSim()) {
            return null
        }

        return availableSims.firstOrNull { it.subscriptionId == simSubscription }
    }

    fun getNumberFromSimSlot(simSlot: Int): String? {
        if (!canHandleDualSim()) {
            return null
        }

        return availableSims
                .firstOrNull { it.simSlotIndex == simSlot }
                ?.let {
                    if (it.number != null && !it.number.isEmpty()) {
                        it.number
                    } else {
                        (it.simSlotIndex + 1).toString() + ""
                    }
                }
    }

    fun getPhoneNumberFromSimSubscription(simSubscription: Int): String? {
        if (simSubscription == 0 || simSubscription == -1 || !canHandleDualSim()) {
            return null
        }

        return availableSims
                .firstOrNull { it.subscriptionId == simSubscription }
                ?.let {
                    if (it.number != null && !it.number.isEmpty()) {
                        it.number
                    } else {
                        (it.simSlotIndex + 1).toString() + ""
                    }
                }
    }

    private fun canHandleDualSim(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1
    }
}
