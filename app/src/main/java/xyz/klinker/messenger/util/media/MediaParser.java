package xyz.klinker.messenger.util.media;

import android.support.annotation.VisibleForTesting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;

public abstract class MediaParser {

    private String matchedText;

    protected abstract String getPatternMatcher();
    protected abstract String getIgnoreMatcher();
    protected abstract String getMimeType();
    protected abstract String buildBody(String matchedText);

    @VisibleForTesting
    public boolean canParse(String text) {
        Matcher matcher = Pattern.compile(getPatternMatcher()).matcher(text);
        if (matcher.find()) {
            matchedText = matcher.group(0);
        }

        return matchedText != null && !Pattern.compile(getIgnoreMatcher()).matcher(text).find();
    }

    public Message parse(long conversationId) {
        Message message = new Message();
        message.conversationId = conversationId;
        message.timestamp = System.currentTimeMillis();
        message.type = Message.TYPE_MEDIA;
        message.read = false;
        message.seen = false;
        message.mimeType = getMimeType();
        message.data = buildBody(matchedText);

        matchedText = null;

        return message;
    }
}
