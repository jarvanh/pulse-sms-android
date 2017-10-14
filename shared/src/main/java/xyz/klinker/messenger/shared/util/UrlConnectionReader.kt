package xyz.klinker.messenger.shared.util

import org.json.JSONObject

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UrlConnectionReader(private val url: String) {

    fun read(): JSONObject? {
        var `object`: JSONObject? = null
        var urlConnection: HttpURLConnection? = null
        try {
            urlConnection = URL(url).openConnection() as HttpURLConnection

            val responseCode = urlConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = readStream(urlConnection.inputStream)
                `object` = JSONObject(response)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect()
            }
        }

        return `object`
    }

    @Throws(Exception::class)
    private fun readStream(stream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(stream))
        val response = StringBuilder()

        var line = reader.readLine()
        while (line != null) {
            response.append(line)
            line = reader.readLine()
        }

        reader.close()
        stream.closeSilent()
        return response.toString()
    }
}
