package xyz.klinker.messenger.shared.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class NonStandardUriUtils {

    public static Map<String, String> getQueryParams(String url) {
        Map<String, String> params = new HashMap<>();
        String[] urlParts = url.split("\\?");
        if (urlParts.length < 2) {
            return params;
        }

        String query = urlParts[1];
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            try {
                String key = URLDecoder.decode(pair[0], "UTF-8");
                String value = "";
                if (pair.length > 1) {
                    value = URLDecoder.decode(pair[1], "UTF-8");
                }

                // skip ?& and &&
                if ("".equals(key) && pair.length == 1) {
                    continue;
                }

                params.put(key, value);
            } catch (UnsupportedEncodingException e) {

            }
        }

        return params;
    }

}
