package xyz.klinker.messenger.util.media;

import android.content.Context;

import java.util.regex.Pattern;

import xyz.klinker.messenger.data.FeatureFlags;
import xyz.klinker.messenger.data.model.Message;
import xyz.klinker.messenger.util.media.parsers.ArticleParser;
import xyz.klinker.messenger.util.media.parsers.YoutubeParser;

/**
 * Create an instance that can be used to parse media from the message text
 */
public class MediaMessageParserFactory {

    public MediaParser getInstance(Context context, String messageText) {
        MediaParser[] parsers = buildParsers(context);

        for (MediaParser parser : parsers) {
            if (parser.canParse(messageText)) {
                return parser;
            }
        }

        return null;
    }

    private MediaParser[] buildParsers(Context context) {
        return new MediaParser[] {
                new YoutubeParser(context),
                new ArticleParser(context)
        };
    }
}
