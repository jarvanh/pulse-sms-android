package xyz.klinker.messenger.fragment.message.send

import android.os.Build
import android.view.View
import android.widget.TextView
import xyz.klinker.messenger.R
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.shared.util.MessageCountHelper

class MessageCounterCalculator(private val fragment: MessageListFragment) {

    private val messageEntry: TextView by lazy { fragment.rootView!!.findViewById<View>(R.id.message_entry) as TextView }
    private val counter: TextView by lazy { fragment.rootView!!.findViewById<View>(R.id.text_counter) as TextView }

    fun updateCounterText() {
        if (fragment.attachManager.attachedUri == null && !fragment.argManager.isGroup && !ignoreCounterText()) {
            val text = messageEntry.text.toString()
            counter.text = MessageCountHelper.getMessageCounterText(text)
        } else {
            counter.text = null
        }
    }

    private fun ignoreCounterText(): Boolean {
        // they seem to have issues, where some dialog pops up, asking which SIM to send from
        // happens when the user is running LineageOS
        return !Account.primary && (Build.MODEL == "Nexus 9" || Build.MANUFACTURER.toLowerCase() == "oneplus" ||
                Build.MANUFACTURER.toLowerCase() == "sony" || Build.MANUFACTURER.toLowerCase() == "xiaomi" ||
                Build.MANUFACTURER.toLowerCase() == "samsung" || Build.MANUFACTURER.toLowerCase() == "lge" ||
                Build.MODEL.toLowerCase().contains("kindle") || Build.MODEL.toLowerCase().contains("YT-X703F"))
    }
}