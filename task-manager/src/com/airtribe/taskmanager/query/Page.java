package com.airtribe.taskmanager.query;

import java.util.Collections;
import java.util.List;

/** One page of results plus the numbers a client needs to ask for the next page. */
public final class Page<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final int totalItems;
    private final int totalPages;

    private Page(List<T> items, int page, int pageSize, int totalItems, int totalPages) {
        this.items = Collections.unmodifiableList(items);
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
    }

    /**
     * Cuts one page out of the full, already sorted list. Page 1 is the first page.
     * A page after the last one is not an error: it simply has no items.
     */
    public static <T> Page<T> of(List<T> all, int page, int pageSize) {
        if (page < 1 || pageSize < 1) {
            throw new IllegalArgumentException("page and pageSize must be at least 1");
        }
        int total = all.size();
        int totalPages = (total + pageSize - 1) / pageSize;
        long from = (long) (page - 1) * pageSize;
        List<T> items = from >= total
                ? List.of()
                : List.copyOf(all.subList((int) from, (int) Math.min(from + pageSize, total)));
        return new Page<>(items, page, pageSize, total, totalPages);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
