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

package xyz.klinker.messenger.shared.data;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MimeTypeTest {

    @Test
    public void shouldIgnoreCase() {
        assertTrue(MimeType.INSTANCE.isVcard("text/VCard"));
    }

    @Test
    public void textPlainSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getTEXT_PLAIN()));
    }

    @Test
    public void textHtmlNotSupported() {
        assertFalse(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getTEXT_HTML()));
    }

    @Test
    public void textVcardSuppported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getTEXT_VCARD()));
    }

    @Test
    public void textXVcardSuppported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getTEXT_X_VCARD()));
    }

    @Test
    public void textXVcalendarSuppported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getTEXT_X_VCALENDAR()));
    }

    @Test
    public void textDirectorySuppported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getTEXT_DIRECTORY()));
    }

    @Test
    public void textDirectoryVcardProfileSuppported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getTEXT_DIRECTORY_VCARD_PROFILE()));
    }

    @Test
    public void applicationVcardSuppported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getAPPLICATION_VCARD()));
    }

    @Test
    public void imageJpegSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getIMAGE_JPEG()));
    }

    @Test
    public void imageBmpSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getIMAGE_BMP()));
    }

    @Test
    public void imageJpgSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getIMAGE_JPG()));
    }

    @Test
    public void imagePngSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getIMAGE_PNG()));
    }

    @Test
    public void imageGifSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getIMAGE_GIF()));
    }

    @Test
    public void videoMpegSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getVIDEO_MPEG()));
    }

    @Test
    public void video3gppSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getVIDEO_3GPP()));
    }

    @Test
    public void videoMp4Supported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getVIDEO_MP4()));
    }

    @Test
    public void audioMp3Supported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getAUDIO_MP3()));
    }

    @Test
    public void audioMp4Supported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getAUDIO_MP4()));
    }

    @Test
    public void audioOggSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getAUDIO_OGG()));
    }

    @Test
    public void audio3gpSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getAUDIO_3GP()));
    }

    @Test
    public void audioAmrSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getAUDIO_AMR()));
    }

    @Test
    public void audioWavSupported() {
        assertTrue(MimeType.INSTANCE.isSupported(MimeType.INSTANCE.getAUDIO_WAV()));
    }

    @Test
    public void isVcard() {
        assertTrue(MimeType.INSTANCE.isVcard(MimeType.INSTANCE.getTEXT_VCARD()));
        assertTrue(MimeType.INSTANCE.isVcard(MimeType.INSTANCE.getTEXT_X_VCARD()));
        assertTrue(MimeType.INSTANCE.isVcard(MimeType.INSTANCE.getTEXT_X_VCALENDAR()));
        assertTrue(MimeType.INSTANCE.isVcard(MimeType.INSTANCE.getTEXT_DIRECTORY()));
        assertTrue(MimeType.INSTANCE.isVcard(MimeType.INSTANCE.getTEXT_DIRECTORY_VCARD_PROFILE()));
        assertTrue(MimeType.INSTANCE.isVcard(MimeType.INSTANCE.getAPPLICATION_VCARD()));
        assertFalse(MimeType.INSTANCE.isVcard(MimeType.INSTANCE.getTEXT_PLAIN()));
        assertFalse(MimeType.INSTANCE.isVcard(MimeType.INSTANCE.getTEXT_HTML()));
        assertFalse(MimeType.INSTANCE.isVcard(MimeType.INSTANCE.getIMAGE_JPEG()));
    }

    @Test
    public void isStaticImage() {
        assertTrue(MimeType.INSTANCE.isStaticImage(MimeType.INSTANCE.getIMAGE_BMP()));
        assertTrue(MimeType.INSTANCE.isStaticImage(MimeType.INSTANCE.getIMAGE_JPEG()));
        assertTrue(MimeType.INSTANCE.isStaticImage(MimeType.INSTANCE.getIMAGE_JPG()));
        assertTrue(MimeType.INSTANCE.isStaticImage(MimeType.INSTANCE.getIMAGE_PNG()));
        assertFalse(MimeType.INSTANCE.isStaticImage(MimeType.INSTANCE.getTEXT_PLAIN()));
        assertFalse(MimeType.INSTANCE.isStaticImage(MimeType.INSTANCE.getTEXT_VCARD()));
        assertFalse(MimeType.INSTANCE.isStaticImage(MimeType.INSTANCE.getTEXT_X_VCARD()));
        assertFalse(MimeType.INSTANCE.isStaticImage(MimeType.INSTANCE.getIMAGE_GIF()));
        assertFalse(MimeType.INSTANCE.isStaticImage(MimeType.INSTANCE.getTEXT_PLAIN()));
        assertFalse(MimeType.INSTANCE.isStaticImage(MimeType.INSTANCE.getVIDEO_MP4()));
        assertFalse(MimeType.INSTANCE.isStaticImage(MimeType.INSTANCE.getAUDIO_MP4()));
    }

    @Test
    public void isVideo() {
        assertTrue(MimeType.INSTANCE.isVideo(MimeType.INSTANCE.getVIDEO_MP4()));
        assertTrue(MimeType.INSTANCE.isVideo(MimeType.INSTANCE.getVIDEO_3GPP()));
        assertTrue(MimeType.INSTANCE.isVideo(MimeType.INSTANCE.getVIDEO_MPEG()));
        assertFalse(MimeType.INSTANCE.isVideo(MimeType.INSTANCE.getIMAGE_JPEG()));
        assertFalse(MimeType.INSTANCE.isVideo(MimeType.INSTANCE.getTEXT_PLAIN()));
        assertFalse(MimeType.INSTANCE.isVideo(MimeType.INSTANCE.getTEXT_VCARD()));
        assertFalse(MimeType.INSTANCE.isVideo(MimeType.INSTANCE.getTEXT_X_VCARD()));
        assertFalse(MimeType.INSTANCE.isVideo(MimeType.INSTANCE.getIMAGE_GIF()));
        assertFalse(MimeType.INSTANCE.isVideo(MimeType.INSTANCE.getAUDIO_MP4()));
    }

    @Test
    public void isAudio() {
        assertTrue(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getAUDIO_MP4()));
        assertTrue(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getAUDIO_MP3()));
        assertTrue(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getAUDIO_MP3_2()));
        assertTrue(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getAUDIO_OGG()));
        assertTrue(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getAUDIO_WAV()));
        assertTrue(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getAUDIO_AMR()));
        assertTrue(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getAUDIO_3GP()));
        assertFalse(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getIMAGE_JPEG()));
        assertFalse(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getTEXT_PLAIN()));
        assertFalse(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getTEXT_VCARD()));
        assertFalse(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getTEXT_X_VCARD()));
        assertFalse(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getIMAGE_GIF()));
        assertFalse(MimeType.INSTANCE.isAudio(MimeType.INSTANCE.getVIDEO_MP4()));
    }

    @Test
    public void extensionText() {
        assertEquals(".txt", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getTEXT_PLAIN()));
    }

    @Test
    public void extensionHtml() {
        assertEquals(".html", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getTEXT_HTML()));
    }

    @Test
    public void extensionVcard() {
        assertEquals(".vcf", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getTEXT_VCARD()));
        assertEquals(".vcf", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getTEXT_X_VCARD()));
        assertEquals(".vcf", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getTEXT_X_VCALENDAR()));
        assertEquals(".vcf", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getTEXT_DIRECTORY()));
        assertEquals(".vcf", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getTEXT_DIRECTORY_VCARD_PROFILE()));
        assertEquals(".vcf", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getAPPLICATION_VCARD()));
    }

    @Test
    public void extensionJpg() {
        assertEquals(".jpg", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getIMAGE_JPEG()));
        assertEquals(".jpg", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getIMAGE_JPG()));
    }

    @Test
    public void extensionBmp() {
        assertEquals(".bmp", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getIMAGE_BMP()));
    }

    @Test
    public void extensionPng() {
        assertEquals(".png", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getIMAGE_PNG()));
    }

    @Test
    public void extensionGif() {
        assertEquals(".gif", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getIMAGE_GIF()));
    }

    @Test
    public void extensionMp4() {
        assertEquals(".mp4", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getVIDEO_MPEG()));
        assertEquals(".mp4", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getVIDEO_MP4()));
        assertEquals(".mp4", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getAUDIO_MP4()));
    }

    @Test
    public void extension3gpp() {
        assertEquals(".3gpp", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getVIDEO_3GPP()));
    }

    @Test
    public void extensionMp3() {
        assertEquals(".mp3", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getAUDIO_MP3()));
        assertEquals(".mp3", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getAUDIO_MP3_2()));
    }

    @Test
    public void extensionOgg() {
        assertEquals(".ogg", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getAUDIO_OGG()));
    }

    @Test
    public void extensionWav() {
        assertEquals(".wav", MimeType.INSTANCE.getExtension(MimeType.INSTANCE.getAUDIO_WAV()));
    }

    @Test
    public void supportsExpandedMedia() {
        assertThat(MimeType.INSTANCE.isExpandedMedia("media/youtube"), Matchers.is(true));
        assertThat(MimeType.INSTANCE.isExpandedMedia("media/twitter"), Matchers.is(true));
        assertThat(MimeType.INSTANCE.isExpandedMedia("media/web"), Matchers.is(true));
    }

}