package xyz.klinker.messenger.shared.util

import java.util.HashSet

object SetUtils {

    fun stringify(set: Set<String>?): String {
        val builder = StringBuilder()

        if (set != null) {
            for (s in set) {
                builder.append(s)
                builder.append(",")
            }
        }

        if (builder.length > 0) {
            builder.deleteCharAt(builder.length - 1)
        }

        return builder.toString()
    }

    fun createSet(stringified: String?): Set<String> {
        val strings = HashSet<String>()

        if (stringified != null && !stringified.isEmpty()) {
            strings += stringified.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        }

        return strings
    }
}
