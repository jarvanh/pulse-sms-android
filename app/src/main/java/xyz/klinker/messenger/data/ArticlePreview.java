package xyz.klinker.messenger.data;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import xyz.klinker.android.article.data.Article;

public class ArticlePreview {

    private static final String JSON_TITLE = "title";
    private static final String JSON_DESCRIPTION = "description";
    private static final String JSON_IMAGE_URL = "image_url";
    private static final String JSON_DOMAIN = "domain";
    private static final String JSON_WEB_URL = "web_url";

    @Nullable
    public static ArticlePreview build(Article article) {
        ArticlePreview preview = new ArticlePreview();

        if (article == null || !article.isArticle) {
            return null;
        }

        preview.title = article.title;
        preview.description = article.description;
        preview.imageUrl = article.image;
        preview.domain = article.domain;
        preview.webUrl = article.url;

        return preview;
    }

    @Nullable
    public static ArticlePreview build(String articleJson) {
        try {
            JSONObject json = new JSONObject(articleJson);

            ArticlePreview preview = new ArticlePreview();
            preview.title = json.getString(JSON_TITLE);
            preview.description = json.getString(JSON_DESCRIPTION);
            preview.imageUrl = json.getString(JSON_IMAGE_URL);
            preview.domain = json.getString(JSON_DOMAIN);
            preview.webUrl = json.getString(JSON_WEB_URL);

            return preview;
        } catch (JSONException e) {
            return null;
        }
    }

    public String title;
    public String description;
    public String imageUrl;
    public String domain;
    public String webUrl;

    @Override
    public String toString() {
        JSONObject json = serialize();

        if (json != null) {
            return json.toString();
        } else {
            return "";
        }
    }

    private JSONObject serialize() {
        try {
            JSONObject json = new JSONObject();
            json.put(JSON_TITLE, title);
            json.put(JSON_DESCRIPTION, description);
            json.put(JSON_IMAGE_URL, imageUrl);
            json.put(JSON_DOMAIN, domain);
            json.put(JSON_WEB_URL, webUrl);

            return json;
        } catch (JSONException e) {
            return null;
        }
    }
}
