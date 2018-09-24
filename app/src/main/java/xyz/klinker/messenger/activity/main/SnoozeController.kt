package xyz.klinker.messenger.activity.main

import android.content.res.ColorStateList
import androidx.appcompat.widget.PopupMenu
import android.view.View
import android.widget.ImageButton
import xyz.klinker.messenger.R
import xyz.klinker.messenger.activity.MessengerActivity
import xyz.klinker.messenger.api.implementation.Account
import xyz.klinker.messenger.api.implementation.ApiUtils
import xyz.klinker.messenger.fragment.bottom_sheet.CustomSnoozeFragment
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.TimeUtils

@Suppress("DEPRECATION")
class SnoozeController(private val activity: MessengerActivity) {

    fun initSnooze() {
        val snooze = activity.findViewById<View>(R.id.snooze) as ImageButton? ?: return

        if (!ColorUtils.isColorDark(Settings.mainColorSet.colorDark)) {
            snooze.imageTintList = ColorStateList.valueOf(activity.resources.getColor(R.color.lightToolbarTextColor))
        }

        snooze.setOnClickListener { view ->
            val menu = PopupMenu(activity, view)
            val currentlySnoozed = Settings.snooze > TimeUtils.now
            menu.inflate(if (currentlySnoozed) R.menu.snooze_off else R.menu.snooze)
            menu.setOnMenuItemClickListener { item ->
                val snoozeTil: Long
                when (item.itemId) {
                    R.id.menu_snooze_off -> snoozeTil = TimeUtils.now
                    R.id.menu_snooze_1 -> snoozeTil = TimeUtils.now + 1000 * 60 * 60
                    R.id.menu_snooze_2 -> snoozeTil = TimeUtils.now + 1000 * 60 * 60 * 2
                    R.id.menu_snooze_4 -> snoozeTil = TimeUtils.now + 1000 * 60 * 60 * 4
                    R.id.menu_snooze_8 -> snoozeTil = TimeUtils.now + 1000 * 60 * 60 * 8
                    R.id.menu_snooze_24 -> snoozeTil = TimeUtils.now + 1000 * 60 * 60 * 24
                    R.id.menu_snooze_72 -> snoozeTil = TimeUtils.now + 1000 * 60 * 60 * 72
                    R.id.menu_snooze_custom -> {
                        val fragment = CustomSnoozeFragment()
                        fragment.show(activity.supportFragmentManager, "")
                        snoozeTil = TimeUtils.now
                    }
                // fall through to the default
                    else -> snoozeTil = TimeUtils.now
                }

                Settings.setValue(activity.applicationContext,
                        activity.getString(R.string.pref_snooze), snoozeTil)
                ApiUtils.updateSnooze(Account.accountId, snoozeTil)
                updateSnoozeIcon()

                true
            }

            menu.show()
        }
    }

    fun updateSnoozeIcon() {
        val currentlySnoozed = Settings.snooze > TimeUtils.now
        val snooze = activity.findViewById<View>(R.id.snooze) as ImageButton?

        if (currentlySnoozed) snooze?.setImageResource(R.drawable.ic_snoozed)
        else snooze?.setImageResource(R.drawable.ic_snooze)
    }

}