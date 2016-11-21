package xyz.klinker.messenger.util.media.parsers;

import android.net.Uri;

import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.media.MediaParser;

public class YoutubeParser extends MediaParser {
    @Override
    protected String getPatternMatcher() {
        return "(youtu.be\\/[^?\\s]*)|(youtube.com\\/watch\\?v=[^&\\s]*)";
    }

    @Override
    protected String getIgnoreMatcher() {
        return "channel|user|playlist";
    }

    @Override
    protected String getMimeType() {
        return MimeType.MEDIA_YOUTUBE;
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

        return "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg";
    }

    public static String getVideoUriFromThumbnail(String thumbnail) {
        Uri uri = Uri.parse(thumbnail);
        String id = uri.getPathSegments().get(1);
        return "https://youtube.com/watch?v=" + id;
    }
}
