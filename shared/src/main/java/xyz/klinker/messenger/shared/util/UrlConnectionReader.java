package xyz.klinker.messenger.shared.util;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UrlConnectionReader {

    private String url;

    public UrlConnectionReader(String url) {
        this.url = url;
    }

    public JSONObject read() {
        JSONObject object = null;
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection)
                    new URL(url).openConnection();

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String response = readStream(urlConnection.getInputStream());
                object = new JSONObject(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return object;
    }

    private String readStream(InputStream stream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder response = new StringBuilder();

        String line = "";
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();
        stream.close();
        return response.toString();
    }
}
