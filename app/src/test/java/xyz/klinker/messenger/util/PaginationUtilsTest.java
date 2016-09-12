package xyz.klinker.messenger.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import xyz.klinker.messenger.MessengerSuite;

import static org.junit.Assert.*;

public class PaginationUtilsTest extends MessengerSuite {

    @Test
    public void emptySourceList() {
        List<String> source = new ArrayList<>();
        List<List<String>> paginatedLists = PaginationUtils.getPages(source, 100);

        assertEquals(0, paginatedLists.size());
    }

    @Test
    public void singlePage() {
        List<String> source = new ArrayList<>();
        source.add("test 1");
        source.add("test 2");

        List<List<String>> paginatedLists = PaginationUtils.getPages(source, 2);
        assertEquals(1, paginatedLists.size());
        assertEquals(2, paginatedLists.get(0).size());
        assertEquals("test 1", paginatedLists.get(0).get(0));
        assertEquals("test 2", paginatedLists.get(0).get(1));

        paginatedLists = PaginationUtils.getPages(source, 8);
        assertEquals(1, paginatedLists.size());
        assertEquals(2, paginatedLists.get(0).size());
        assertEquals("test 1", paginatedLists.get(0).get(0));
        assertEquals("test 2", paginatedLists.get(0).get(1));
    }

    @Test
    public void multiplePages_oneItemEach() {
        List<String> source = new ArrayList<>();
        source.add("test 1");
        source.add("test 2");

        List<List<String>> paginatedLists = PaginationUtils.getPages(source, 1);
        assertEquals(2, paginatedLists.size());
        assertEquals(1, paginatedLists.get(0).size());
        assertEquals(1, paginatedLists.get(1).size());
        assertEquals("test 1", paginatedLists.get(0).get(0));
        assertEquals("test 2", paginatedLists.get(1).get(0));
    }

    @Test
    public void multiplePages_multipleItemEach() {
        List<String> source = new ArrayList<>();
        source.add("test 1");
        source.add("test 2");
        source.add("test 3");
        source.add("test 4");
        source.add("test 5");
        source.add("test 6");

        List<List<String>> paginatedLists = PaginationUtils.getPages(source, 2);
        assertEquals(3, paginatedLists.size());
        assertEquals(2, paginatedLists.get(0).size());
        assertEquals(2, paginatedLists.get(1).size());
        assertEquals(2, paginatedLists.get(2).size());
        assertEquals("test 1", paginatedLists.get(0).get(0));
        assertEquals("test 2", paginatedLists.get(0).get(1));
        assertEquals("test 3", paginatedLists.get(1).get(0));
        assertEquals("test 4", paginatedLists.get(1).get(1));
        assertEquals("test 5", paginatedLists.get(2).get(0));
        assertEquals("test 6", paginatedLists.get(2).get(1));
    }

    @Test
    public void multiplePages_unevenEndPage() {
        List<String> source = new ArrayList<>();
        source.add("test 1");
        source.add("test 2");
        source.add("test 3");
        source.add("test 4");
        source.add("test 5");

        List<List<String>> paginatedLists = PaginationUtils.getPages(source, 3);
        assertEquals(2, paginatedLists.size());
        assertEquals(3, paginatedLists.get(0).size());
        assertEquals(2, paginatedLists.get(1).size());
        assertEquals("test 1", paginatedLists.get(0).get(0));
        assertEquals("test 2", paginatedLists.get(0).get(1));
        assertEquals("test 3", paginatedLists.get(0).get(2));
        assertEquals("test 4", paginatedLists.get(1).get(0));
        assertEquals("test 5", paginatedLists.get(1).get(1));
    }
}