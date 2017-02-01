package xyz.klinker.messenger.shared.data;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class YouTubePreview {

    private static final String JSON_TITLE = "title";
    private static final String JSON_THUMBNAIL = "thumbnail";
    private static final String JSON_URL = "url";

    @Nullable
    public static YouTubePreview build(JSONObject apiResponse) {
        YouTubePreview preview = new YouTubePreview();

        if (apiResponse == null) {
            return null;
        }

        try {
            JSONObject video = apiResponse.getJSONArray("items").getJSONObject(0);
            String videoId = video.getString("id");

            preview.title = video.getJSONObject("snippet").getString("title");
            preview.thumbnail = "https://img.youtube.com/vi/" + videoId + "/0.jpg";
            preview.url = "https://youtube.com/watch?v=" + videoId;

            return preview;
        } catch (JSONException e) {
            return null;
        }
    }

    @Nullable
    public static YouTubePreview build(String youtubeJson) {
        try {
            JSONObject json = new JSONObject(youtubeJson);

            YouTubePreview preview = new YouTubePreview();
            preview.title = json.getString(JSON_TITLE);
            preview.thumbnail = json.getString(JSON_THUMBNAIL);
            preview.url = json.getString(JSON_URL);

            return preview;
        } catch (JSONException e) {
            return null;
        }
    }

    public String title;
    public String thumbnail;
    public String url;

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
            json.put(JSON_THUMBNAIL, thumbnail);
            json.put(JSON_URL, url);

            return json;
        } catch (JSONException e) {
            return null;
        }
    }
}