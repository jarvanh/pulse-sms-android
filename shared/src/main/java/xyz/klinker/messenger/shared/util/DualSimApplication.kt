package xyz.klinker.messenger.shared.util

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import android.view.View

import xyz.klinker.messenger.shared.R
import xyz.klinker.messenger.shared.data.DataSource
import xyz.klinker.messenger.shared.data.model.Conversation
import xyz.klinker.messenger.shared.view.ViewBadger

@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
class DualSimApplication(private val switchSim: View) {

    private val context: Context = switchSim.context

    fun apply(conversationId: Long) {
        var visible = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subscriptions = DualSimUtils.availableSims

            if (subscriptions.size > 1) {
                visible = true
                switchSim.visibility = View.VISIBLE
                val badger = ViewBadger(context, switchSim)
                val conversation = DataSource.getConversation(context, conversationId)

                var set = false
                if (conversation?.simSubscriptionId != null) {
                    for (i in subscriptions.indices) {
                        if (subscriptions[i].subscriptionId == conversation.simSubscriptionId) {
                            set = true
                            badger.text = (subscriptions[i].simSlotIndex + 1).toString()
                        }
                    }
                }

                if (!set) {
                    // show one for default
                    badger.text = ""
                }

                switchSim.setOnClickListener { _ -> showSimSelection(subscriptions, conversation, badger) }
            }
        }

        if (!visible && switchSim.visibility != View.GONE) {
            switchSim.visibility = View.GONE
            switchSim.isEnabled = false
        }
    }

    private fun showSimSelection(subscriptions: List<SubscriptionInfo>?, conversation: Conversation?, badger: ViewBadger) {
        val active = arrayOfNulls<CharSequence>(1 + subscriptions!!.size)
        var selected = 0
        active[0] = context.getString(R.string.default_text)

        for (i in subscriptions.indices) {
            val info = subscriptions[i]

            active[i + 1] = formatSimString(info)
            if (conversation!!.simSubscriptionId != null && info.subscriptionId == conversation.simSubscriptionId) {
                selected = i + 1
            }
        }

        AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.select_sim))
                .setSingleChoiceItems(active, selected) { dialogInterface, i ->
                    if (i == 0) {
                        conversation!!.simSubscriptionId = -1
                        badger.text = ""
                    } else {
                        conversation!!.simSubscriptionId = subscriptions[i - 1].subscriptionId
                        badger.text = (i - 1).toString()
                    }

                    DataSource.updateConversationSettings(context, conversation)
                    dialogInterface.dismiss()
                }.show()
    }

    private fun formatSimString(info: SubscriptionInfo) = when {
        info.displayName != null -> "SIM " + (info.simSlotIndex + 1) + ": " + info.displayName
        info.number != null -> "SIM " + (info.simSlotIndex + 1) + ": " + info.number
        else -> "SIM Slot " + (info.simSlotIndex + 1)
    }

}
