package xyz.klinker.messenger.shared.util.media.parsers;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.model.Message;

import static org.junit.Assert.*;

public class YoutubeParserTest extends MessengerRobolectricSuite {

    private YoutubeParser parser;

    @Before
    public void setUp() {
        parser = new YoutubeParser(null);
    }

    @Test
    public void mimeType() {
        assertThat(parser.getMimeType(), CoreMatchers.is(MimeType.INSTANCE.getMEDIA_YOUTUBE_V2()));
    }

    @Test
    public void shouldParseVideoUrl() {
        assertThat(parser.canParse(makeMessage("youtu.be/")), CoreMatchers.is(true));
        assertThat(parser.canParse(makeMessage("youtube.com/")), CoreMatchers.is(true));
        assertThat(parser.canParse(makeMessage("https://youtu.be/ohGXwX6zKow")), CoreMatchers.is(true));
        assertThat(parser.canParse(makeMessage("https://www.youtube.com/watch?v=ohGXwX6zKow")), CoreMatchers.is(true));
    }

    @Test
    public void shouldNotParseNonVideoUrl() {
        assertThat(parser.canParse(makeMessage("https://www.youtube.com/channel/UCycZgQlj27nwMyGSihghSig/videos")), CoreMatchers.is(false));
        assertThat(parser.canParse(makeMessage("https://www.youtube.com/channel/UCycZgQlj27nwMyGSihghSig/playlist")), CoreMatchers.is(false));
        assertThat(parser.canParse(makeMessage("https://www.youtube.com/user/klinker24")), CoreMatchers.is(false));
    }

    @Test
    public void shouldConvertThumbnailBackToVideoUri() {
        assertThat(YoutubeParser.Companion.getVideoUriFromThumbnail("https://img.youtube.com/vi/ohGXwX6zKow/maxresdefault.jpg"),
                CoreMatchers.is("https://youtube.com/watch?v=ohGXwX6zKow"));
        assertThat(YoutubeParser.Companion.getVideoUriFromThumbnail("https://img.youtube.com/vi/ohGXwX6zKow/0.jpg"),
                CoreMatchers.is("https://youtube.com/watch?v=ohGXwX6zKow"));
    }

    private Message makeMessage(String text) {
        Message m = new Message();
        m.setData(text);
        m.setMimeType(MimeType.INSTANCE.getTEXT_PLAIN());

        return m;
    }
}
