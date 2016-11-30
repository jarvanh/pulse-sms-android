package xyz.klinker.messenger.util.media.parsers;

import android.content.Context;
import android.net.Uri;

import java.util.regex.Pattern;

import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.media.MediaParser;

public class YoutubeParser extends MediaParser {

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

        return "https://img.youtube.com/vi/" + videoId + "/0.jpg";
    }

    public static String getVideoUriFromThumbnail(String thumbnail) {
        Uri uri = Uri.parse(thumbnail);
        String id = uri.getPathSegments().get(1);
        return "https://youtube.com/watch?v=" + id;
    }
}
