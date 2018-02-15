package xyz.klinker.messenger.shared.data

import org.json.JSONException
import org.json.JSONObject

import xyz.klinker.android.article.data.Article

class ArticlePreview {

    var title: String? = null
    var description: String? = null
    var imageUrl: String? = null
    var domain: String? = null
    var webUrl: String? = null

    override fun toString(): String {
        val json = serialize()
        return json?.toString() ?: ""
    }

    private fun serialize() = try {
        val json = JSONObject()
        json.put(JSON_TITLE, title)
        json.put(JSON_DESCRIPTION, description)
        json.put(JSON_IMAGE_URL, imageUrl)
        json.put(JSON_DOMAIN, domain)
        json.put(JSON_WEB_URL, webUrl)

        json
    } catch (e: JSONException) {
        null
    }

    companion object {

        private const val JSON_TITLE = "title"
        private const val JSON_DESCRIPTION = "description"
        private const val JSON_IMAGE_URL = "image_url"
        private const val JSON_DOMAIN = "domain"
        private const val JSON_WEB_URL = "web_url"

        fun build(article: Article?): ArticlePreview? {
            val preview = ArticlePreview()

            if (article == null || !article.isArticle) {
                return null
            }

            preview.title = article.title
            preview.description = article.description
            preview.imageUrl = article.image
            preview.domain = article.domain
            preview.webUrl = article.url

            return preview
        }

        fun build(articleJson: String) = try {
            val json = JSONObject(articleJson)

            val preview = ArticlePreview()
            preview.title = json.getString(JSON_TITLE)
            preview.description = json.getString(JSON_DESCRIPTION)
            preview.imageUrl = json.getString(JSON_IMAGE_URL)
            preview.domain = json.getString(JSON_DOMAIN)
            preview.webUrl = json.getString(JSON_WEB_URL)

            preview
        } catch (e: JSONException) {
            null
        }
    }
}
