package xyz.klinker.messenger.shared.data.pojo

enum class VibratePattern constructor(var pattern: LongArray?) {
    OFF(null), DEFAULT(null),
    TWO_LONG(longArrayOf(0, 1000, 500, 1000)),
    TWO_SHORT(longArrayOf(0, 300, 200, 300)),
    THREE_SHORT(longArrayOf(0, 300, 200, 300, 200, 300)),
    ONE_SHORT_ONE_LONG(longArrayOf(0, 300, 300, 1000)),
    ONE_LONG_ONE_SHORT(longArrayOf(0, 1000, 300, 300)),
    ONE_LONG(longArrayOf(0, 1000)),
    ONE_SHORT(longArrayOf(0, 300)),
    ONE_EXTRA_LONG(longArrayOf(0, 3500)),
    TWO_SHORT_ONE_LONG(longArrayOf(0, 225, 50, 225, 50, 500)),
    ONE_LONG_ONE_SHORT_ONE_LONG(longArrayOf(0, 500, 50, 225, 50, 500))
}