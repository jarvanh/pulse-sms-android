package xyz.klinker.messenger.shared.util;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import xyz.klinker.messenger.MessengerSuite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SetUtilsTest extends MessengerSuite {

    @Test
    public void shouldStringifyDoesntGiveOrder() {
        Set<String> set = new HashSet<>();
        set.add("test 1");
        set.add("test 2");

        String result = SetUtils.INSTANCE.stringify(set);
        assertTrue(result.contains("test 1"));
        assertTrue(result.contains("test 2"));
        assertEquals("", result.replace("test 1", "").replace("test 2", "").replace(",", ""));
    }

    @Test
    public void shouldStringifyEmptySet() {
        Set<String> set = new HashSet<>();
        assertEquals("", SetUtils.INSTANCE.stringify(set));
    }

    @Test
    public void shouldStringifyNullSet() {
        Set<String> set = null;
        assertEquals("", SetUtils.INSTANCE.stringify(set));
    }

    @Test
    public void shouldCreateSet() {
        String str = "test 1,test 2";
        Set<String> set = SetUtils.INSTANCE.createSet(str);

        assertTrue(set.contains("test 1"));
        assertTrue(set.contains("test 2"));

        set.remove("test 1");
        set.remove("test 2");

        assertEquals(0, set.size());
    }

    @Test
    public void shouldCreateSetFromEmptyString() {
        assertEquals(0, SetUtils.INSTANCE.createSet("").size());
    }

    @Test
    public void shouldCreateSetFromNullString() {
        assertEquals(0, SetUtils.INSTANCE.createSet(null).size());
    }
}
