package com.airtribe.taskmanager.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What to list: filters, sort order and page. Every field is optional, which is why it is built
 * with a Builder: {@code TaskQuery.builder().completed(false).search("rent").page(2).build()}.
 *
 * <p>The query holds the raw values a client sent ({@code priority}, {@code sortBy} and
 * {@code order} are Strings) so the service can validate them and answer 400 for bad values.
 * Extra {@link TaskFilter}s can be added with {@link Builder#filter(TaskFilter)} without changing
 * this class.
 */
public final class TaskQuery {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;

    private final Boolean completed;
    private final String priority;
    private final String search;
    private final String sortBy;
    private final String order;
    private final int page;
    private final int pageSize;
    private final List<TaskFilter> extraFilters;

    private TaskQuery(Builder builder) {
        this.completed = builder.completed;
        this.priority = builder.priority;
        this.search = builder.search;
        this.sortBy = builder.sortBy;
        this.order = builder.order;
        this.page = builder.page;
        this.pageSize = builder.pageSize;
        this.extraFilters = Collections.unmodifiableList(new ArrayList<>(builder.extraFilters));
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A query with no filters: first page of 10, oldest first. */
    public static TaskQuery all() {
        return builder().build();
    }

    /** null = both completed and open tasks. */
    public Boolean getCompleted() {
        return completed;
    }

    /** null = any priority. Otherwise must be LOW, MEDIUM or HIGH. */
    public String getPriority() {
        return priority;
    }

    /** null or blank = no text search. */
    public String getSearch() {
        return search;
    }

    /** null = "createdAt". Otherwise the name of a registered {@link SortStrategy}. */
    public String getSortBy() {
        return sortBy;
    }

    /** null = "asc". Otherwise "asc" or "desc". */
    public String getOrder() {
        return order;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public List<TaskFilter> getExtraFilters() {
        return extraFilters;
    }

    public static final class Builder {
        private Boolean completed;
        private String priority;
        private String search;
        private String sortBy;
        private String order;
        private int page = DEFAULT_PAGE;
        private int pageSize = DEFAULT_PAGE_SIZE;
        private final List<TaskFilter> extraFilters = new ArrayList<>();

        private Builder() {
        }

        public Builder completed(Boolean completed) {
            this.completed = completed;
            return this;
        }

        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        public Builder search(String search) {
            this.search = search;
            return this;
        }

        public Builder sortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public Builder order(String order) {
            this.order = order;
            return this;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /** Adds any custom filter. All filters must match for a task to be included. */
        public Builder filter(TaskFilter filter) {
            if (filter == null) {
                throw new IllegalArgumentException("filter must not be null");
            }
            this.extraFilters.add(filter);
            return this;
        }

        public TaskQuery build() {
            return new TaskQuery(this);
        }
    }
}
