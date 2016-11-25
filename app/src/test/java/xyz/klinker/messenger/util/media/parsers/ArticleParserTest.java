package xyz.klinker.messenger.util.media.parsers;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.data.MimeType;
import xyz.klinker.messenger.data.model.Message;

import static org.junit.Assert.*;

public class ArticleParserTest extends MessengerRobolectricSuite {

    private ArticleParser parser;

    @Before
    public void setUp() {
        parser = new ArticleParser(null);
    }

    @Test
    public void mimeType() {
        assertThat(parser.getMimeType(), Matchers.is(MimeType.MEDIA_ARTICLE));
    }

    @Test
    public void shouldParseWebUrl() {
        assertThat(parser.canParse("klinkerapps.com"), Matchers.is(true));

        setUp();
        assertThat(parser.canParse("https://www.klinkerapps.com"), Matchers.is(true));

        setUp();
        assertThat(parser.canParse("http://klinkerapps.com"), Matchers.is(true));

        setUp();
        assertThat(parser.canParse("https://example.com/testing?testing+again"), Matchers.is(true));
    }

    @Test
    public void shouldNotParseNonWebText() {
        assertThat(parser.canParse("dont match"), Matchers.is(false));

        setUp();
        assertThat(parser.canParse("test/co"), Matchers.is(false));

        setUp();
        assertThat(parser.canParse("hey.fomda"), Matchers.is(false));
    }
}