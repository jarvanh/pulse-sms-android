package xyz.klinker.messenger.util.media;

import android.content.Context;

import java.util.regex.Pattern;

import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.media.parsers.YoutubeParser;

/**
 * Create an instance that can be used to parse media from the message text
 */
public class MediaMessageParserFactory {

    private static final MediaParser[] parsers = new MediaParser[] {
            new YoutubeParser()
    };

    public MediaParser getInstance(Context context, String messageText) {
        if (!FeatureFlags.get(context).ARTICLE_ENHANCER) {
            return null;
        } else {
            for (MediaParser parser : parsers) {
                if (parser.canParse(messageText)) {
                    return parser;
                }
            }
        }

        return null;
    }
}
