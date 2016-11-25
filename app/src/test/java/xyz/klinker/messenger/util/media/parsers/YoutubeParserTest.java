package xyz.klinker.messenger.util.media.parsers;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.MessengerSuite;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;

import static org.junit.Assert.*;

public class YoutubeParserTest extends MessengerRobolectricSuite {

    private YoutubeParser parser;

    @Before
    public void setUp() {
        parser = new YoutubeParser(null);
    }

    @Test
    public void mimeType() {
        assertThat(parser.getMimeType(), Matchers.is(MimeType.MEDIA_YOUTUBE));
    }

    @Test
    public void shouldParseVideoUrl() {
        assertThat(parser.canParse("youtu.be/"), Matchers.is(true));
        assertThat(parser.canParse("youtube.com/"), Matchers.is(true));
        assertThat(parser.canParse("https://youtu.be/ohGXwX6zKow"), Matchers.is(true));
        assertThat(parser.canParse("https://www.youtube.com/watch?v=ohGXwX6zKow"), Matchers.is(true));
    }

    @Test
    public void shouldNotParseNonVideoUrl() {
        assertThat(parser.canParse("https://www.youtube.com/channel/UCycZgQlj27nwMyGSihghSig/videos"), Matchers.is(false));
        assertThat(parser.canParse("https://www.youtube.com/channel/UCycZgQlj27nwMyGSihghSig/playlist"), Matchers.is(false));
        assertThat(parser.canParse("https://www.youtube.com/user/klinker24"), Matchers.is(false));
    }

    @Test
    public void shouldParseBodyOfYoutubeLink() {
        parser.canParse("https://www.youtube.com/watch?v=ohGXwX6zKow");
        Message message = parser.parse(1);

        assertThat(message.data, Matchers.is("https://img.youtube.com/vi/ohGXwX6zKow/maxresdefault.jpg"));
    }

    @Test
    public void shouldParseShortenedYoutubeLink() {
        parser.canParse("https://www.youtu.be/ohGXwX6zKow");
        Message message = parser.parse(1);

        assertThat(message.data, Matchers.is("https://img.youtube.com/vi/ohGXwX6zKow/maxresdefault.jpg"));
    }

    @Test
    public void shouldConvertThumbnailBackToVideoUri() {
        assertThat(YoutubeParser.getVideoUriFromThumbnail("https://img.youtube.com/vi/ohGXwX6zKow/maxresdefault.jpg"),
                Matchers.is("https://youtube.com/watch?v=ohGXwX6zKow"));
    }
}