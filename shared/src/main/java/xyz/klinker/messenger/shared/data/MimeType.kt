/*
 * Copyright (C) 2020 Luke Klinker
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

package xyz.klinker.messenger.shared.data

import android.content.Context
import xyz.klinker.messenger.shared.R
import java.util.HashMap

/**
 * Holds the current mime types that the app supports.
 */
object MimeType {

    val TEXT_PLAIN = "text/plain"
    val TEXT_HTML = "text/html"
    val TEXT_VCARD = "text/vcard"
    val TEXT_X_VCARD = "text/x-vcard"
    val TEXT_X_VCALENDAR = "text/x-vcalendar"
    val TEXT_DIRECTORY = "text/directory"
    val TEXT_DIRECTORY_VCARD_PROFILE = "text/directory;profile=vCard"
    val APPLICATION_VCARD = "application/vcard"
    val IMAGE_JPEG = "image/jpeg"
    val IMAGE_BMP = "image/bmp"
    val IMAGE_JPG = "image/jpg"
    val IMAGE_PNG = "image/png"
    val IMAGE_GIF = "image/gif"
    val VIDEO_MPEG = "video/mpeg"
    val VIDEO_3GPP = "video/3gpp"
    val VIDEO_MP4 = "video/mp4"
    val AUDIO_MP3 = "audio/mpeg"
    val AUDIO_MP3_2 = "audio/mp3"
    val AUDIO_MP4 = "audio/mp4"
    val AUDIO_OGG = "audio/ogg"
    val AUDIO_WAV = "audio/vnd.wav"
    val AUDIO_3GP = "audio/3gp"
    val AUDIO_AMR = "audio/amr"
    val MEDIA_YOUTUBE_V2 = "media/youtube-v2"
    val MEDIA_ARTICLE = "media/web"
    val MEDIA_TWITTER = "media/twitter"
    val MEDIA_MAP = "media/map"

    private val extensions: MutableMap<String, String>

    init {
        extensions = HashMap()
        extensions.put(TEXT_PLAIN, "txt")
        extensions.put(TEXT_HTML, "html")
        extensions.put(TEXT_VCARD, "vcf")
        extensions.put(TEXT_X_VCARD, "vcf")
        extensions.put(TEXT_X_VCALENDAR, "vcf")
        extensions.put(TEXT_DIRECTORY, "vcf")
        extensions.put(TEXT_DIRECTORY_VCARD_PROFILE, "vcf")
        extensions.put(APPLICATION_VCARD, "vcf")
        extensions.put(IMAGE_JPEG, "jpg")
        extensions.put(IMAGE_BMP, "bmp")
        extensions.put(IMAGE_JPG, "jpg")
        extensions.put(IMAGE_PNG, "png")
        extensions.put(IMAGE_GIF, "gif")
        extensions.put(VIDEO_MPEG, "mp4")
        extensions.put(VIDEO_3GPP, "3gpp")
        extensions.put(VIDEO_MP4, "mp4")
        extensions.put(AUDIO_MP3, "mp3")
        extensions.put(AUDIO_MP3_2, "mp3")
        extensions.put(AUDIO_MP4, "mp4")
        extensions.put(AUDIO_OGG, "ogg")
        extensions.put(AUDIO_WAV, "wav")
        extensions.put(AUDIO_3GP, "3gp")
        extensions.put(AUDIO_AMR, "amr")
    }

    /**
     * Checks whether the provided mime type is supported.
     *
     * @param mimeType the type to check.
     * @return true if supported, otherwise false.
     */
    fun isSupported(mimeType: String): Boolean {
        var mimeType = mimeType
        mimeType = mimeType.toLowerCase()

        return mimeType == TEXT_PLAIN ||
                mimeType == TEXT_VCARD ||
                mimeType == TEXT_X_VCARD ||
                mimeType == TEXT_X_VCALENDAR ||
                mimeType.contains("vcard") ||
                mimeType == TEXT_DIRECTORY ||
                mimeType == TEXT_DIRECTORY_VCARD_PROFILE ||
                mimeType == APPLICATION_VCARD ||
                mimeType == IMAGE_JPEG ||
                mimeType == IMAGE_BMP ||
                mimeType == IMAGE_JPG ||
                mimeType == IMAGE_PNG ||
                mimeType == IMAGE_GIF ||
                mimeType == VIDEO_MPEG ||
                mimeType == VIDEO_3GPP ||
                mimeType == VIDEO_MP4 ||
                mimeType == AUDIO_MP3 ||
                mimeType == AUDIO_MP3_2 ||
                mimeType == AUDIO_MP4 ||
                mimeType == AUDIO_OGG ||
                mimeType == AUDIO_WAV ||
                mimeType.contains(AUDIO_3GP) ||
                mimeType.contains(AUDIO_AMR)
    }

    /**
     * Gets whether the mime type is a vcard extension.
     */
    fun isVcard(mimeType: String): Boolean {
        var mimeType = mimeType
        mimeType = mimeType.toLowerCase()
        return mimeType.contains("vcard") || mimeType == TEXT_VCARD || mimeType == TEXT_X_VCARD || mimeType == TEXT_X_VCALENDAR ||
                mimeType == TEXT_DIRECTORY || mimeType == TEXT_DIRECTORY_VCARD_PROFILE || mimeType == APPLICATION_VCARD
    }

    /**
     * Gets whether the mime type is a supported static image (not a gif).
     */
    fun isStaticImage(mimeType: String?): Boolean {
        var mimeType: String? = mimeType ?: return false

        mimeType = mimeType!!.toLowerCase()
        return mimeType.startsWith("image/") && mimeType != IMAGE_GIF
    }

    /**
     * Gets whether the mime type is a video file.
     */
    fun isVideo(mimeType: String): Boolean {
        var mimeType = mimeType
        mimeType = mimeType.toLowerCase()
        return mimeType.startsWith("video/")
    }

    /**
     * Gets whether the mime type is an audio file.
     */
    fun isAudio(mimeType: String): Boolean {
        var mimeType = mimeType
        mimeType = mimeType.toLowerCase()
        return mimeType.startsWith("audio/")
    }

    fun isExpandedMedia(mimeType: String?): Boolean {
        var mimeType = mimeType
        mimeType = if (mimeType != null) mimeType.toLowerCase() else null
        return mimeType != null && mimeType.startsWith("media")
    }

    /**
     * Gets the file extension for a mime type. For example, .jpg for a JPEG.
     */
    fun getExtension(mimeType: String): String {
        return "." + extensions[mimeType]
    }

    fun getTextDescription(context: Context, mimeType: String?): String {
        return when {
            mimeType == null -> ""
            MimeType.isAudio(mimeType) -> context.getString(R.string.audio_message)
            MimeType.isVideo(mimeType) -> context.getString(R.string.video_message)
            MimeType.isVcard(mimeType) -> context.getString(R.string.contact_card)
            MimeType.isStaticImage(mimeType) -> context.getString(R.string.picture_message)
            mimeType == MimeType.IMAGE_GIF -> context.getString(R.string.gif_message)
            MimeType.isExpandedMedia(mimeType) -> context.getString(R.string.media)
            else -> ""
        }
    }

}
