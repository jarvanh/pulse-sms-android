package xyz.klinker.messenger.shared.util.media.parsers;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import xyz.klinker.messenger.MessengerRobolectricSuite;
import xyz.klinker.messenger.shared.data.MimeType;
import xyz.klinker.messenger.shared.data.model.Message;

import static org.junit.Assert.*;

public class ArticleParserTest extends MessengerRobolectricSuite {

    private ArticleParser parser;

    @Before
    public void setUp() {
        parser = new ArticleParser(null);
    }

    @Test
    public void mimeType() {
        assertThat(parser.getMimeType(), Matchers.is(MimeType.INSTANCE.getMEDIA_ARTICLE()));
    }

    @Test
    public void shouldParseWebUrl() {
        assertThat(parser.canParse(makeMessage("klinkerapps.com")), Matchers.is(true));

        setUp();
        assertThat(parser.canParse(makeMessage("https://www.klinkerapps.com")), Matchers.is(true));

        setUp();
        assertThat(parser.canParse(makeMessage("http://klinkerapps.com")), Matchers.is(true));

        setUp();
        assertThat(parser.canParse(makeMessage("https://example.com/testing?testing+again")), Matchers.is(true));
    }

    @Test
    public void shouldNotParseNonWebText() {
        assertThat(parser.canParse(makeMessage("dont match")), Matchers.is(false));

        setUp();
        assertThat(parser.canParse(makeMessage("test/co")), Matchers.is(false));

        setUp();
        assertThat(parser.canParse(makeMessage("hey.fomda")), Matchers.is(false));
    }

    private Message makeMessage(String text) {
        Message m = new Message();
        m.setData(text);
        m.setMimeType(MimeType.INSTANCE.getTEXT_PLAIN());

        return m;
    }
}