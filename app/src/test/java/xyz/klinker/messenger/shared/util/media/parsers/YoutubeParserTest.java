package xyz.klinker.messenger.shared.util.media.parsers;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.shared.data.MimeType;

import static org.junit.Assert.*;

public class YoutubeParserTest extends MessengerRobolectricSuite {

    private YoutubeParser parser;

    @Before
    public void setUp() {
        parser = new YoutubeParser(null);
    }

    @Test
    public void mimeType() {
        assertThat(parser.getMimeType(), Matchers.is(MimeType.INSTANCE.getMEDIA_YOUTUBE_V2()));
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
    public void shouldConvertThumbnailBackToVideoUri() {
        assertThat(YoutubeParser.getVideoUriFromThumbnail("https://img.youtube.com/vi/ohGXwX6zKow/maxresdefault.jpg"),
                Matchers.is("https://youtube.com/watch?v=ohGXwX6zKow"));
        assertThat(YoutubeParser.getVideoUriFromThumbnail("https://img.youtube.com/vi/ohGXwX6zKow/0.jpg"),
                Matchers.is("https://youtube.com/watch?v=ohGXwX6zKow"));
    }
}
