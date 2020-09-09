package xyz.klinker.messenger.shared.service.notification

import xyz.klinker.messenger.shared.util.TimeUtils

interface ShortcutUpdater {

    fun refreshDynamicShortcuts(delay: Long = 10 * TimeUtils.SECOND)

}