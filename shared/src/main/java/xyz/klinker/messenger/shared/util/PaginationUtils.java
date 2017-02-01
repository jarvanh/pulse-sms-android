package xyz.klinker.messenger.shared.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PaginationUtils {

    /**
     * Creates a list of lists. Each entry in the list is page of results.
     *
     * @param collection the original collection of objects, unpaginated
     * @param pageSize the max size each page should be
     * @param <T> The type of object you are paginating
     * @return Paginated list
     */
    public static <T> List<List<T>> getPages(Collection<T> collection, int pageSize) {
        List<T> list = new ArrayList<T>(collection);

        if (pageSize > list.size()) {
            pageSize = list.size();
        }

        int numPages = (int) Math.ceil((double) list.size() / (double) pageSize);
        List<List<T>> pages = new ArrayList<>(numPages);

        for (int pageNum = 0; pageNum < numPages;) {
            pages.add(
                    list.subList(pageNum * pageSize, Math.min(++pageNum * pageSize, list.size()))
            );
        }

        return pages;
    }
}
