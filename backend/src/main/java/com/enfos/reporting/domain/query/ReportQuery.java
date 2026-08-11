package com.enfos.reporting.domain.query;

import java.util.List;

/**
 * A client's request against a single report: free-text search, column filters (AND
 * across entries), column sorts, and pagination. Column keys and sort directions arrive
 * as raw, unvalidated strings — validation against a report's declared columns happens in
 * the application layer, not here (see {@code QueryValidator}).
 */
public record ReportQuery(
        String search,
        List<FilterCriterion> filters,
        List<SortSpec> sorts,
        int page,
        int size
) {

    public ReportQuery {
        filters = filters == null ? List.of() : List.copyOf(filters);
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }

    public static ReportQuery of(
            String search,
            List<FilterCriterion> filters,
            List<SortSpec> sorts,
            int page,
            int size
    ) {
        return new ReportQuery(search, filters, sorts, page, size);
    }
}
