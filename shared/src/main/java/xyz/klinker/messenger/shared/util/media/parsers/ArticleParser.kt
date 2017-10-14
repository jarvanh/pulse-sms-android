package xyz.klinker.messenger.shared.util.media.parsers

import android.content.Context

import java.util.regex.Pattern

import xyz.klinker.android.article.ArticleUtils
import xyz.klinker.android.article.data.Article
import xyz.klinker.messenger.shared.BuildConfig
import xyz.klinker.messenger.shared.data.ArticlePreview
import xyz.klinker.messenger.shared.data.MimeType
import xyz.klinker.messenger.shared.util.Regex
import xyz.klinker.messenger.shared.util.media.MediaParser

class ArticleParser(context: Context?) : MediaParser(context) {

    override val patternMatcher: Pattern
        get() = Regex.WEB_URL

    override val ignoreMatcher: String?
        get() = null

    public override val mimeType: String
        get() = MimeType.MEDIA_ARTICLE

    override fun buildBody(matchedText: String?): String? {
        val utils = ArticleUtils(ARTICLE_API_KEY)
        val article = utils.fetchArticle(context, matchedText)

        val preview = ArticlePreview.build(article)
        return if (preview != null && article != null && article.isArticle && article.image != null &&
                article.title != null && !article.title.isEmpty() &&
                article.description != null && !article.description.isEmpty() &&
                preview.title != null && !preview.title!!.isEmpty() &&
                preview.description != null && !preview.description!!.isEmpty())

            preview.toString()
        else
            null
    }

    companion object {
        val ARTICLE_API_KEY = BuildConfig.ARTICLE_API_KEY
    }
}
