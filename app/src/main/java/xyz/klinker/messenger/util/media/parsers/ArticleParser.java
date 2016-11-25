package xyz.klinker.messenger.util.media.parsers;

import android.content.Context;
import android.net.Uri;

import xyz.klinker.android.article.ArticleLoadedListener;
import xyz.klinker.android.article.ArticleUtils;
import xyz.klinker.android.article.data.Article;
import xyz.klinker.messenger.BuildConfig;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.util.Regex;
import xyz.klinker.messenger.util.media.MediaParser;

public class ArticleParser extends MediaParser {

    public ArticleParser(Context context) {
        super(context);
    }

    @Override
    protected String getPatternMatcher() {
        return Regex.WEB_URL.toString();
    }

    @Override
    protected String getIgnoreMatcher() {
        return null;
    }

    @Override
    protected String getMimeType() {
        return MimeType.MEDIA_ARTICLE;
    }

    @Override
    protected String buildBody(String matchedText) {
        ArticleUtils utils = new ArticleUtils(BuildConfig.ARTICLE_API_KEY);
        utils.preloadArticle(context, matchedText, null);

        return null;
    }
}
