package xyz.klinker.messenger.shared.util.media.parsers

import android.content.Context
import android.net.Uri

import org.json.JSONObject

import java.util.regex.Pattern

import xyz.klinker.messenger.shared.BuildConfig
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.data.YouTubePreview
import xyz.klinker.messenger.shared.util.UrlConnectionReader
import xyz.klinker.messenger.shared.util.media.MediaParser

class YoutubeParser(context: Context?) : MediaParser(context) {

    override val patternMatcher: Pattern
        get() = Pattern.compile("(youtu.be\\/[^?\\s]*)|(youtube.com\\/watch\\?v=[^&\\s]*)")

    override val ignoreMatcher: String?
        get() = "channel|user|playlist"

    public override val mimeType: String
        get() = MimeType.MEDIA_YOUTUBE_V2

    override fun buildBody(matchedText: String?): String? {
        val uri = Uri.parse(matchedText)

        var videoId: String? = uri.getQueryParameter("v")
        if (videoId == null) {
            videoId = uri.getQueryParameter("ci")
            if (videoId == null) {
                videoId = uri.lastPathSegment
            }
        }

        if (videoId == null) {
            return null
        }

        val apiResponse = queryApi(videoId)
        val preview = YouTubePreview.build(apiResponse)
        return preview?.toString()
    }

    private fun queryApi(videoId: String): JSONObject? {
        return UrlConnectionReader(buildUrlForVideo(videoId)).read()
    }

    private fun buildUrlForVideo(videoId: String): String {
        return YOUTUBE_API_REQUEST.replace("<video-id>", videoId)
    }

    companion object {
        private val YOUTUBE_API_REQUEST = "https://www.googleapis.com/youtube/v3/videos" +
                "?id=<video-id>" +
                "&key=" + BuildConfig.YOUTUBE_API_KEY +
                "&fields=items(id,snippet(channelId,title,categoryId),statistics)" +
                "&part=snippet,statistics"

        fun getVideoUriFromThumbnail(thumbnail: String): String {
            val uri = Uri.parse(thumbnail)
            val id = uri.pathSegments[1]
            return "https://youtube.com/watch?v=" + id
        }
    }
}
