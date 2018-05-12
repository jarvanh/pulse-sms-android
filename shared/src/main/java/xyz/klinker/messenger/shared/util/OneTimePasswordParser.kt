package xyz.klinker.messenger.shared.util

private class OtpMatcher(val regex: String, val captureGroup: Int, val validationRegex: String? = null)

object OneTimePasswordParser {

    private val matchers = listOf(
            OtpMatcher(
                    "(?-i)([a-z]-|[\"\'\\(])?([A-Z0-9]{4,8})[\"\'\\)]?(?i)(\\s+is(\\s+your)?)?(\\s+facebook|\\s+messenger){0,2}(\\s+[^\\s]+){0,2}(\\s+(otp|sms|secret|safepass|unique\\s+id|secure|security|authorization|authentication|access|login|verification|confirmation|check|password\\s+reset|one-time|identification|activation|registration|validation)){1,3}\\s+code",
                    2, "[0-9A-Z]*[0-9][0-9A-Z]*"
            ), OtpMatcher(
                    "(?i)((otp|sms|secret|safepass|unique\\s+id|secure|security|authorization|authentication|access|login|verification|confirmation|check|password\\s+reset|one-time|identification|activation|registration|validation)\\s+){1,3}(pass)?code(\\s+(for(\\s+[^\\s]+){1,3}|you\\s+requested))?(\\s+is:?|:)?\\s+(?-i)([a-z]-|[\"\'\\(])?([A-Z0-9]{4,8})[\"\'\\)]?(\\s|\\.|,|$)",
                    9, "[0-9A-Z]*[0-9][0-9A-Z]*"
            ), OtpMatcher(
                    "^(?-i)([a-z]-|[\"\'\\(])?([A-Z0-9]{4,8})[\"\'\\)]?(?i)\\s+is\\s+your\\s+([^\\s]+\\s+)?(code|account\\s+key|otp)(\\.|\\s+for|\\s+to|$)",
                    2, "[0-9A-Z]*[0-9][0-9A-Z]*"
            ), OtpMatcher(
                    "^(?i)([^\\s]+\\s+)?your\\s+([^\\s]+\\s+){0,2}code(\\s+is:?|:)\\s+(?-i)([a-z]-|[\"\'\\(])?([A-Z0-9]{4,8})[\"\'\\)]?(\\s|\\.|$)",
                    5, "[0-9A-Z]*[0-9][0-9A-Z]*"
            ), OtpMatcher(
                    "(?i)(enter|use)\\s+(the\\s+|this\\s+)?([^\\s]+\\s+)?code:?\\s+(?-i)([a-z]-|[\"\'\\(])?([A-Z0-9]{4,8})[\"\'\\)]?(?i)\\s+to\\s+(confirm|verify)",
                    5, "[0-9A-Z]*[0-9][0-9A-Z]*"
            ), OtpMatcher(
                    "^(?i)([^\\s]+\\s+)?code(\\s+is:?|:)?\\s+(?-i)([a-z]-|[\"\'\\(])?([A-Z0-9]{4,8})[\"\'\\)]?(\\.|\\s|$)",
                    4, "[0-9A-Z]*[0-9][0-9A-Z]*"
            ), OtpMatcher(
                    "^(?i)use\\s+(?-i)([A-Z0-9]{4,8})(?i)\\s+as(\\s+your)?(\\s+microsoft\\s+account|\\s+instagram)(\\s+[^\\s]+){0,2}\\s+code",
                    1, "[0-9A-Z]*[0-9][0-9A-Z]*"
            ), OtpMatcher(
                    "^(?i)snapchat\\s+code:\\s+(?-i)([A-Z0-9]{4,8})\\.",
                    1, "[0-9A-Z]*[0-9][0-9A-Z]*"
            ), OtpMatcher(
                    "^(?i)enter\\s+this\\s+code\\s+to\\s+reset\\s+your\\s+twitter\\s+password:\\s+(?-i)([A-Z0-9]{4,8})\\.?",
                    1, "[0-9A-Z]*[0-9][0-9A-Z]*"
            ), OtpMatcher(
                    "^(?i)use\\s+(?-i)([0-9]{4,8})(?i)\\s+as\\s+your\\s+password\\s+for",
                    1
            ), OtpMatcher(
                    "^(?i)your\\s+whatsapp\\s+code\\s+is\\s+([0-9]{3}-[0-9]{3})",
                    1
            ), OtpMatcher(
                    "(?i)([0-9]{4,8})\\s+is\\s+your\\s+uber\\s+code",
                    1
            )
    )

    fun getOtp(text: String): String? {
        matchers.forEach {
            val regex = it.regex.toRegex()
            val matchResult = regex.find(text)

            if (matchResult != null) {
                val groups = matchResult.groupValues
                if (groups.size >= it.captureGroup) {
                    return groups[it.captureGroup]
                }
            }
        }

        return null
    }
}