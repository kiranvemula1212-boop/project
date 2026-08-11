package com.enfos.reporting.infrastructure.inmemory;

import com.enfos.reporting.domain.model.Page;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.port.ReportDataSource;
import com.enfos.reporting.domain.query.ReportQuery;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * A {@link ReportDataSource} backed by an immutable in-memory list of rows. Filters,
 * sorts, then slices — in that order, since filtering after sorting or slicing before
 * computing totals are both real bug classes despite looking harmless.
 */
public final class InMemoryReportDataSource implements ReportDataSource {

    private final List<ReportRow> rows;

    public InMemoryReportDataSource(List<ReportRow> rows) {
        this.rows = List.copyOf(rows);
    }

    @Override
    public Page<ReportRow> fetch(ReportDefinition definition, ReportQuery query) {
        Predicate<ReportRow> predicate = RowPredicateFactory.searchPredicate(definition, query.search())
                .and(RowPredicateFactory.filterPredicate(query.filters()));

        List<ReportRow> filtered = rows.stream().filter(predicate).toList();

        Comparator<ReportRow> comparator = RowComparatorFactory.comparatorFor(definition, query.sorts());
        List<ReportRow> sorted = filtered.stream().sorted(comparator).toList();

        // Total reflects the filtered set, computed BEFORE slicing to a page — the
        // client needs to know how many rows matched, not how many fit on this page.
        long totalElements = sorted.size();

        long fromIndexLong = (long) query.page() * query.size();
        int fromIndex = (int) Math.min(fromIndexLong, sorted.size());
        int toIndex = Math.min(fromIndex + query.size(), sorted.size());
        List<ReportRow> content = sorted.subList(fromIndex, toIndex);

        return new Page<>(content, query.page(), query.size(), totalElements);
    }
}
