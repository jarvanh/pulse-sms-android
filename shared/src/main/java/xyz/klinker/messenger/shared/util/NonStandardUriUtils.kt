package xyz.klinker.messenger.shared.util

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.HashMap

object NonStandardUriUtils {

    fun getQueryParams(url: String): Map<String, String> {
        val params = HashMap<String, String>()
        val urlParts = url.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (urlParts.size < 2) {
            return params
        }

        val query = urlParts[1]
        for (param in query.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val pair = param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            try {
                val key = URLDecoder.decode(pair[0], "UTF-8")
                var value = ""
                if (pair.size > 1) {
                    value = URLDecoder.decode(pair[1], "UTF-8")
                }

                // skip ?& and &&
                if ("" == key && pair.size == 1) {
                    continue
                }

                params.put(key, value)
            } catch (e: UnsupportedEncodingException) {

            }

        }

        return params
    }

}
