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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MimeTypeTest {

    @Test
    public void textPlainSupported() {
        assertTrue(MimeType.isSupported(MimeType.TEXT_PLAIN));
    }

    @Test
    public void textHtmlNotSupported() {
        assertFalse(MimeType.isSupported(MimeType.TEXT_HTML));
    }

    @Test
    public void textVcardSuppported() {
        assertTrue(MimeType.isSupported(MimeType.TEXT_VCARD));
    }

    @Test
    public void textXVcardSuppported() {
        assertTrue(MimeType.isSupported(MimeType.TEXT_X_VCARD));
    }

    @Test
    public void imageJpegSupported() {
        assertTrue(MimeType.isSupported(MimeType.IMAGE_JPEG));
    }

    @Test
    public void imageBmpSupported() {
        assertTrue(MimeType.isSupported(MimeType.IMAGE_BMP));
    }

    @Test
    public void imageJpgSupported() {
        assertTrue(MimeType.isSupported(MimeType.IMAGE_JPG));
    }

    @Test
    public void imagePngSupported() {
        assertTrue(MimeType.isSupported(MimeType.IMAGE_PNG));
    }

    @Test
    public void imageGifSupported() {
        assertTrue(MimeType.isSupported(MimeType.IMAGE_GIF));
    }

    @Test
    public void videoMpegSupported() {
        assertTrue(MimeType.isSupported(MimeType.VIDEO_MPEG));
    }

    @Test
    public void video3gppSupported() {
        assertTrue(MimeType.isSupported(MimeType.VIDEO_3GPP));
    }

    @Test
    public void videoMp4Supported() {
        assertTrue(MimeType.isSupported(MimeType.VIDEO_MP4));
    }

    @Test
    public void audioMp3Supported() {
        assertTrue(MimeType.isSupported(MimeType.AUDIO_MP3));
    }

    @Test
    public void audioMp4Supported() {
        assertTrue(MimeType.isSupported(MimeType.AUDIO_MP4));
    }

    @Test
    public void audioOggSupported() {
        assertTrue(MimeType.isSupported(MimeType.AUDIO_OGG));
    }

    @Test
    public void audioWavSupported() {
        assertTrue(MimeType.isSupported(MimeType.AUDIO_WAV));
    }

    @Test
    public void isVcard() {
        assertTrue(MimeType.isVcard(MimeType.TEXT_VCARD));
        assertTrue(MimeType.isVcard(MimeType.TEXT_X_VCARD));
        assertFalse(MimeType.isVcard(MimeType.TEXT_PLAIN));
        assertFalse(MimeType.isVcard(MimeType.TEXT_HTML));
        assertFalse(MimeType.isVcard(MimeType.IMAGE_JPEG));
    }

    @Test
    public void isStaticImage() {
        assertTrue(MimeType.isStaticImage(MimeType.IMAGE_BMP));
        assertTrue(MimeType.isStaticImage(MimeType.IMAGE_JPEG));
        assertTrue(MimeType.isStaticImage(MimeType.IMAGE_JPG));
        assertTrue(MimeType.isStaticImage(MimeType.IMAGE_PNG));
        assertFalse(MimeType.isStaticImage(MimeType.TEXT_PLAIN));
        assertFalse(MimeType.isStaticImage(MimeType.TEXT_VCARD));
        assertFalse(MimeType.isStaticImage(MimeType.TEXT_X_VCARD));
        assertFalse(MimeType.isStaticImage(MimeType.IMAGE_GIF));
        assertFalse(MimeType.isStaticImage(MimeType.TEXT_PLAIN));
        assertFalse(MimeType.isStaticImage(MimeType.VIDEO_MP4));
        assertFalse(MimeType.isStaticImage(MimeType.AUDIO_MP4));
    }

    @Test
    public void isVideo() {
        assertTrue(MimeType.isVideo(MimeType.VIDEO_MP4));
        assertTrue(MimeType.isVideo(MimeType.VIDEO_3GPP));
        assertTrue(MimeType.isVideo(MimeType.VIDEO_MPEG));
        assertFalse(MimeType.isVideo(MimeType.IMAGE_JPEG));
        assertFalse(MimeType.isVideo(MimeType.TEXT_PLAIN));
        assertFalse(MimeType.isVideo(MimeType.TEXT_VCARD));
        assertFalse(MimeType.isVideo(MimeType.TEXT_X_VCARD));
        assertFalse(MimeType.isVideo(MimeType.IMAGE_GIF));
        assertFalse(MimeType.isVideo(MimeType.AUDIO_MP4));
    }

    @Test
    public void isAudio() {
        assertTrue(MimeType.isAudio(MimeType.AUDIO_MP4));
        assertTrue(MimeType.isAudio(MimeType.AUDIO_MP3));
        assertTrue(MimeType.isAudio(MimeType.AUDIO_MP3_2));
        assertTrue(MimeType.isAudio(MimeType.AUDIO_OGG));
        assertTrue(MimeType.isAudio(MimeType.AUDIO_WAV));
        assertFalse(MimeType.isAudio(MimeType.IMAGE_JPEG));
        assertFalse(MimeType.isAudio(MimeType.TEXT_PLAIN));
        assertFalse(MimeType.isAudio(MimeType.TEXT_VCARD));
        assertFalse(MimeType.isAudio(MimeType.TEXT_X_VCARD));
        assertFalse(MimeType.isAudio(MimeType.IMAGE_GIF));
        assertFalse(MimeType.isAudio(MimeType.VIDEO_MP4));
    }

    @Test
    public void extensionText() {
        assertEquals(".txt", MimeType.getExtension(MimeType.TEXT_PLAIN));
    }

    @Test
    public void extensionHtml() {
        assertEquals(".html", MimeType.getExtension(MimeType.TEXT_HTML));
    }

    @Test
    public void extensionVcard() {
        assertEquals(".vcf", MimeType.getExtension(MimeType.TEXT_VCARD));
    }

    @Test
    public void extensionXVcard() {
        assertEquals(".vcf", MimeType.getExtension(MimeType.TEXT_X_VCARD));
    }

    @Test
    public void extensionJpg() {
        assertEquals(".jpg", MimeType.getExtension(MimeType.IMAGE_JPEG));
        assertEquals(".jpg", MimeType.getExtension(MimeType.IMAGE_JPG));
    }

    @Test
    public void extensionBmp() {
        assertEquals(".bmp", MimeType.getExtension(MimeType.IMAGE_BMP));
    }

    @Test
    public void extensionPng() {
        assertEquals(".png", MimeType.getExtension(MimeType.IMAGE_PNG));
    }

    @Test
    public void extensionGif() {
        assertEquals(".gif", MimeType.getExtension(MimeType.IMAGE_GIF));
    }

    @Test
    public void extensionMp4() {
        assertEquals(".mp4", MimeType.getExtension(MimeType.VIDEO_MPEG));
        assertEquals(".mp4", MimeType.getExtension(MimeType.VIDEO_MP4));
        assertEquals(".mp4", MimeType.getExtension(MimeType.AUDIO_MP4));
    }

    @Test
    public void extension3gpp() {
        assertEquals(".3gpp", MimeType.getExtension(MimeType.VIDEO_3GPP));
    }

    @Test
    public void extensionMp3() {
        assertEquals(".mp3", MimeType.getExtension(MimeType.AUDIO_MP3));
        assertEquals(".mp3", MimeType.getExtension(MimeType.AUDIO_MP3_2));
    }

    @Test
    public void extensionOgg() {
        assertEquals(".ogg", MimeType.getExtension(MimeType.AUDIO_OGG));
    }

    @Test
    public void extensionWav() {
        assertEquals(".wav", MimeType.getExtension(MimeType.AUDIO_WAV));
    }

}