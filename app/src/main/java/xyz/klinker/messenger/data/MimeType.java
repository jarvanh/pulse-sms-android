/*
 * Copyright (C) 2016 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the current mime types that the app supports.
 */
public class MimeType {

    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_HTML = "text/html";
    public static final String TEXT_VCARD = "text/vcard";
    public static final String TEXT_X_VCARD = "text/x-vcard";
    public static final String TEXT_X_VCALENDAR = "text/x-vcalendar";
    public static final String TEXT_DIRECTORY = "text/directory";
    public static final String TEXT_DIRECTORY_VCARD_PROFILE = "text/directory;profile=vCard";
    public static final String APPLICATION_VCARD = "application/vcard";
    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String IMAGE_BMP = "image/bmp";
    public static final String IMAGE_JPG = "image/jpg";
    public static final String IMAGE_PNG = "image/png";
    public static final String IMAGE_GIF = "image/gif";
    public static final String VIDEO_MPEG = "video/mpeg";
    public static final String VIDEO_3GPP = "video/3gpp";
    public static final String VIDEO_MP4 = "video/mp4";
    public static final String AUDIO_MP3 = "audio/mpeg";
    public static final String AUDIO_MP3_2 = "audio/mp3";
    public static final String AUDIO_MP4 = "audio/mp4";
    public static final String AUDIO_OGG = "audio/ogg";
    public static final String AUDIO_WAV = "audio/vnd.wav";
    public static final String AUDIO_3GP = "audio/3gp";
    public static final String AUDIO_AMR = "audio/amr";
    public static final String MEDIA_YOUTUBE = "media/youtube";
    public static final String MEDIA_YOUTUBE_V2 = "media/youtube-v2";
    public static final String MEDIA_ARTICLE = "media/web";
    public static final String MEDIA_TWITTER = "media/twitter";

    private static final Map<String, String> extensions;

    static {
        extensions = new HashMap<>();
        extensions.put(TEXT_PLAIN, "txt");
        extensions.put(TEXT_HTML, "html");
        extensions.put(TEXT_VCARD, "vcf");
        extensions.put(TEXT_X_VCARD, "vcf");
        extensions.put(TEXT_X_VCALENDAR, "vcf");
        extensions.put(TEXT_DIRECTORY, "vcf");
        extensions.put(TEXT_DIRECTORY_VCARD_PROFILE, "vcf");
        extensions.put(APPLICATION_VCARD, "vcf");
        extensions.put(IMAGE_JPEG, "jpg");
        extensions.put(IMAGE_BMP, "bmp");
        extensions.put(IMAGE_JPG, "jpg");
        extensions.put(IMAGE_PNG, "png");
        extensions.put(IMAGE_GIF, "gif");
        extensions.put(VIDEO_MPEG, "mp4");
        extensions.put(VIDEO_3GPP, "3gpp");
        extensions.put(VIDEO_MP4, "mp4");
        extensions.put(AUDIO_MP3, "mp3");
        extensions.put(AUDIO_MP3_2, "mp3");
        extensions.put(AUDIO_MP4, "mp4");
        extensions.put(AUDIO_OGG, "ogg");
        extensions.put(AUDIO_WAV, "wav");
        extensions.put(AUDIO_3GP, "3gp");
        extensions.put(AUDIO_AMR, "amr");
    }

    /**
     * Checks whether the provided mime type is supported.
     *
     * @param mimeType the type to check.
     * @return true if supported, otherwise false.
     */
    public static boolean isSupported(String mimeType) {
        return mimeType.equals(TEXT_PLAIN) ||
                mimeType.equals(TEXT_VCARD) ||
                mimeType.equals(TEXT_X_VCARD) ||
                mimeType.equals(TEXT_X_VCALENDAR) ||
                mimeType.equals(TEXT_DIRECTORY) ||
                mimeType.equals(TEXT_DIRECTORY_VCARD_PROFILE) ||
                mimeType.equals(APPLICATION_VCARD) ||
                mimeType.equals(IMAGE_JPEG) ||
                mimeType.equals(IMAGE_BMP) ||
                mimeType.equals(IMAGE_JPG) ||
                mimeType.equals(IMAGE_PNG) ||
                mimeType.equals(IMAGE_GIF) ||
                mimeType.equals(VIDEO_MPEG) ||
                mimeType.equals(VIDEO_3GPP) ||
                mimeType.equals(VIDEO_MP4) ||
                mimeType.equals(AUDIO_MP3) ||
                mimeType.equals(AUDIO_MP3_2) ||
                mimeType.equals(AUDIO_MP4) ||
                mimeType.equals(AUDIO_OGG) ||
                mimeType.equals(AUDIO_WAV) ||
                mimeType.contains(AUDIO_3GP) ||
                mimeType.contains(AUDIO_AMR);
    }

    /**
     * Gets whether the mime type is a vcard extension.
     */
    public static boolean isVcard(String mimeType) {
        return mimeType.equals(TEXT_VCARD) || mimeType.equals(TEXT_X_VCARD) || mimeType.equals(TEXT_X_VCALENDAR) ||
                mimeType.equals(TEXT_DIRECTORY) || mimeType.equals(TEXT_DIRECTORY_VCARD_PROFILE) || mimeType.equals(APPLICATION_VCARD);
    }

    /**
     * Gets whether the mime type is a supported static image (not a gif).
     */
    public static boolean isStaticImage(String mimeType) {
        return mimeType.startsWith("image/") && !mimeType.equals(IMAGE_GIF);
    }

    /**
     * Gets whether the mime type is a video file.
     */
    public static boolean isVideo(String mimeType) {
        return mimeType.startsWith("video/");
    }

    /**
     * Gets whether the mime type is an audio file.
     */
    public static boolean isAudio(String mimeType) {
        return mimeType.startsWith("audio/");
    }

    public static boolean isExpandedMedia(String mimeType) {
        return mimeType != null && mimeType.startsWith("media");
    }

    /**
     * Gets the file extension for a mime type. For example, .jpg for a JPEG.
     */
    public static String getExtension(String mimeType) {
        return "." + extensions.get(mimeType);
    }

}
