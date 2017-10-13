package xyz.klinker.messenger.shared.data.pojo

enum class BaseTheme constructor(var isDark: Boolean) {
    DAY_NIGHT(false), ALWAYS_LIGHT(false),
    ALWAYS_DARK(true), BLACK(true)
}