package xyz.klinker.messenger.util.media;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;

public abstract class MediaParser {

    protected Context context;
    private String matchedText;

    protected abstract Pattern getPatternMatcher();
    protected abstract String getIgnoreMatcher();
    protected abstract String getMimeType();
    protected abstract String buildBody(String matchedText);
    
    public MediaParser(Context context){
        this.context = context;
    }

    @VisibleForTesting
    public boolean canParse(String text) {
        Matcher matcher = getPatternMatcher().matcher(text);
        if (matcher.find()) {
            matchedText = matcher.group(0);
        }

        return matchedText != null && (getIgnoreMatcher() == null ||
                (getIgnoreMatcher() != null && !Pattern.compile(getIgnoreMatcher()).matcher(text).find()));
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

        return message.data == null ? null : message;
    }
}
