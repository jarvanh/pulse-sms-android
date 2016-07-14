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

package xyz.klinker.messenger.util;

/**
 * Holds the current mime types that the app supports.
 */
public class MimeTypeUtil {

    public static final String TEXT_PLAIN = "text/plain";
    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String IMAGE_BMP = "image/bmp";
    public static final String IMAGE_JPG = "image/jpg";
    public static final String IMAGE_PNG = "image/png";
    public static final String IMAGE_GIF = "image/gif";
    public static final String VIDEO_MPEG = "video/mpeg";
    public static final String VIDEO_3GPP = "video/3gpp";
    public static final String VIDEO_MP4 = "video/mp4";

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
                mimeType.equals(IMAGE_GIF);
    }

}
