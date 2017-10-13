package xyz.klinker.messenger.shared.data

import org.json.JSONException
import org.json.JSONObject

class YouTubePreview {

    var title: String? = null
    var thumbnail: String? = null
    var url: String? = null

    override fun toString(): String {
        val json = serialize()
        return json?.toString() ?: ""
    }

    private fun serialize() = try {
        val json = JSONObject()
        json.put(JSON_TITLE, title)
        json.put(JSON_THUMBNAIL, thumbnail)
        json.put(JSON_URL, url)

        json
    } catch (e: JSONException) {
        null
    }

    companion object {

        private val JSON_TITLE = "title"
        private val JSON_THUMBNAIL = "thumbnail"
        private val JSON_URL = "url"

        fun build(apiResponse: JSONObject?): YouTubePreview? {
            val preview = YouTubePreview()

            if (apiResponse == null) {
                return null
            }

            return try {
                val video = apiResponse.getJSONArray("items").getJSONObject(0)
                val videoId = video.getString("id")

                preview.title = video.getJSONObject("snippet").getString("title")
                preview.thumbnail = "https://img.youtube.com/vi/$videoId/0.jpg"
                preview.url = "https://youtube.com/watch?v=" + videoId

                preview
            } catch (e: JSONException) {
                null
            }

        }

        fun build(youtubeJson: String) = try {
            val json = JSONObject(youtubeJson)

            val preview = YouTubePreview()
            preview.title = json.getString(JSON_TITLE)
            preview.thumbnail = json.getString(JSON_THUMBNAIL)
            preview.url = json.getString(JSON_URL)

            preview
        } catch (e: JSONException) {
            null
        }
    }
}