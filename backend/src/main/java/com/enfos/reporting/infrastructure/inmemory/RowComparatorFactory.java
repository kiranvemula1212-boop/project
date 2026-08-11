package com.enfos.reporting.infrastructure.inmemory;

import com.enfos.reporting.domain.model.ColumnDefinition;
import com.enfos.reporting.domain.model.ColumnType;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.model.SortDirection;
import com.enfos.reporting.domain.query.SortSpec;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the comparator chain that backs in-memory sorting.
 */
final class RowComparatorFactory {

    private RowComparatorFactory() {
    }

    static Comparator<ReportRow> comparatorFor(ReportDefinition definition, List<SortSpec> sorts) {
        Comparator<ReportRow> comparator = null;
        for (SortSpec sort : sorts) {
            Comparator<ReportRow> next =
                    (r1, r2) -> compareRowValues(r1, r2, sort.columnKey(), sort.direction());
            comparator = comparator == null ? next : comparator.thenComparing(next);
        }

        // Always append an ascending compare on the identity column, even with no sorts
        // requested. Without this, rows with equal sort keys (or no sort at all) can
        // reorder between page requests, so a user paging through can see a duplicate on
        // page 2 and silently never see another row. This is invisible with a handful of
        // rows and only shows up once ties actually span a page boundary.
        String identityKey = identityColumnKey(definition);
        Comparator<ReportRow> tiebreaker =
                (r1, r2) -> compareRowValues(r1, r2, identityKey, SortDirection.ASC);

        return comparator == null ? tiebreaker : comparator.thenComparing(tiebreaker);
    }

    private static String identityColumnKey(ReportDefinition definition) {
        return definition.columns().stream()
                .filter(column -> column.type() == ColumnType.ID)
                .findFirst()
                .map(ColumnDefinition::key)
                .orElseThrow(() -> new IllegalStateException(
                        "Report '" + definition.id() + "' has no ID column to use as a stable sort tiebreaker."));
    }

    private static int compareRowValues(ReportRow r1, ReportRow r2, String columnKey, SortDirection direction) {
        Object v1 = r1.value(columnKey);
        Object v2 = r2.value(columnKey);

        // Nulls sort last in BOTH directions — resolved before applying direction, so
        // direction only ever affects the ordering of two non-null values. An active
        // project with no end date should not lead the list when sorting by end date.
        if (v1 == null && v2 == null) {
            return 0;
        }
        if (v1 == null) {
            return 1;
        }
        if (v2 == null) {
            return -1;
        }

        int comparison = compareNonNullValues(v1, v2);
        return direction == SortDirection.DESC ? -comparison : comparison;
    }

    @SuppressWarnings("unchecked")
    private static int compareNonNullValues(Object v1, Object v2) {
        if (v1 instanceof Number n1 && v2 instanceof Number n2) {
            return new BigDecimal(n1.toString()).compareTo(new BigDecimal(n2.toString()));
        }
        if (v1 instanceof Comparable<?> comparable && v1.getClass().isInstance(v2)) {
            return ((Comparable<Object>) comparable).compareTo(v2);
        }
        // Fallback to string comparison — covers ISO-8601 DATE strings (which sort
        // correctly lexicographically) and any other type this engine doesn't special-case.
        return String.valueOf(v1).compareTo(String.valueOf(v2));
    }
}
