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

/**
 * Holds the current mime types that the app supports.
 */
public class MimeType {

    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_HTML = "text/html";
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

    /**
     * Checks whether the provided mime type is supported.
     *
     * @param mimeType the type to check.
     * @return true if supported, otherwise false.
     */
    public static boolean isSupported(String mimeType) {
        return mimeType.equals(TEXT_PLAIN) ||
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
                mimeType.equals(AUDIO_WAV);
    }

    /**
     * Gets whether the mime type is a supported static image (not a gif).
     */
    public static boolean isStaticImage(String mimeType) {
        return mimeType.startsWith("image/") && !mimeType.equals(MimeType.IMAGE_GIF);
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
    
}
