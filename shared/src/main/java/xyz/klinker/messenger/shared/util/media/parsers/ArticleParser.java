package xyz.klinker.messenger.shared.util.media.parsers;

import android.content.Context;

import java.util.regex.Pattern;

import xyz.klinker.android.article.ArticleUtils;
import xyz.klinker.android.article.data.Article;
import xyz.klinker.messenger.shared.BuildConfig;
import xyz.klinker.messenger.shared.data.ArticlePreview;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.util.Regex;
import xyz.klinker.messenger.shared.util.media.MediaParser;

public class ArticleParser extends MediaParser {

    public static final String ARTICLE_API_KEY = BuildConfig.ARTICLE_API_KEY;

    public ArticleParser(Context context) {
        super(context);
    }

    @Override
    protected Pattern getPatternMatcher() {
        return Regex.WEB_URL;
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
        ArticleUtils utils = new ArticleUtils(ARTICLE_API_KEY);
        Article article = utils.fetchArticle(context, matchedText);

        ArticlePreview preview = ArticlePreview.build(article);
        return preview != null && article != null && article.isArticle && article.image != null &&
                article.title != null && !article.title.isEmpty() &&
                article.description != null && !article.description.isEmpty() ?

                preview.toString() : null;
    }
}
