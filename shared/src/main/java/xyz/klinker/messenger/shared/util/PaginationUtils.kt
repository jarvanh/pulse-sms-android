package xyz.klinker.messenger.shared.util

import java.util.ArrayList
import java.util.Collections

object PaginationUtils {

    /**
     * Creates a list of lists. Each entry in the list is page of results.
     *
     * @param collection the original collection of objects, unpaginated
     * @param pageSize the max size each page should be
     * @param <T> The type of object you are paginating
     * @return Paginated list
    </T> */
    fun <T> getPages(collection: Collection<T>, pageSize: Int): List<List<T>> {
        var pageSize = pageSize
        val list = ArrayList(collection)

        if (pageSize > list.size) {
            pageSize = list.size
        }

        val numPages = Math.ceil(list.size.toDouble() / pageSize.toDouble()).toInt()
        val pages = ArrayList<List<T>>(numPages)

        var pageNum = 0
        while (pageNum < numPages) {
            pages.add(
                    list.subList(pageNum * pageSize, Math.min(++pageNum * pageSize, list.size))
            )
        }

        return pages
    }
}
