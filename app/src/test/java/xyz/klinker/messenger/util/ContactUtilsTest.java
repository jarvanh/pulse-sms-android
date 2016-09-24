package xyz.klinker.messenger.util;

import org.hamcrest.Matchers;
import org.junit.Test;

import xyz.klinker.messenger.MessengerSuite;

import static org.junit.Assert.*;

public class ContactUtilsTest extends MessengerSuite {

    @Test
    public void shouldConvertLongNumberToPlain() {
        assertThat("5159911493", Matchers.is(ContactUtils.getPlainNumber("+15159911493")));
        assertThat("5159911493", Matchers.is(ContactUtils.getPlainNumber("+5159911493")));
        assertThat("5159911493", Matchers.is(ContactUtils.getPlainNumber("15159911493")));
        assertThat("5159911493", Matchers.is(ContactUtils.getPlainNumber("5159911493")));
        assertThat("9911493", Matchers.is(ContactUtils.getPlainNumber("9911493")));
    }
}