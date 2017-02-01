package xyz.klinker.messenger.shared.util.media.parsers;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;

import org.json.JSONObject;

import java.util.regex.Pattern;

import xyz.klinker.messenger.shared.BuildConfig;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.YouTubePreview;
import xyz.klinker.messenger.shared.util.UrlConnectionReader;
import xyz.klinker.messenger.shared.util.media.MediaParser;

public class YoutubeParser extends MediaParser {

    private static final String YOUTUBE_API_REQUEST = "https://www.googleapis.com/youtube/v3/videos" +
            "?id=<video-id>" +
            "&key=" + BuildConfig.YOUTUBE_API_KEY +
            "&fields=items(id,snippet(channelId,title,categoryId),statistics)" +
            "&part=snippet,statistics";

    public YoutubeParser(Context context) {
        super(context);
    }

    @Override
    protected Pattern getPatternMatcher() {
        return Pattern.compile("(youtu.be\\/[^?\\s]*)|(youtube.com\\/watch\\?v=[^&\\s]*)");
    }

    @Override
    protected String getIgnoreMatcher() {
        return "channel|user|playlist";
    }

    @Override
    protected String getMimeType() {
        return MimeType.MEDIA_YOUTUBE_V2;
    }

    @Override
    protected String buildBody(String matchedText) {
        Uri uri = Uri.parse(matchedText);

        String videoId = uri.getQueryParameter("v");
        if (videoId == null) {
            videoId = uri.getQueryParameter("ci");
            if (videoId == null) {
                videoId = uri.getLastPathSegment();
            }
        }

        JSONObject apiResponse = queryApi(videoId);
        YouTubePreview preview = YouTubePreview.build(apiResponse);
        return preview != null ? preview.toString() : null;
    }

    @VisibleForTesting
    protected JSONObject queryApi(String videoId) {
        return new UrlConnectionReader(buildUrlForVideo(videoId)).read();
    }

    public static String getVideoUriFromThumbnail(String thumbnail) {
        Uri uri = Uri.parse(thumbnail);
        String id = uri.getPathSegments().get(1);
        return "https://youtube.com/watch?v=" + id;
    }

    private String buildUrlForVideo(String videoId) {
        return YOUTUBE_API_REQUEST.replace("<video-id>", videoId);
    }
}
