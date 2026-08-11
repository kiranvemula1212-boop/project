package com.enfos.reporting.infrastructure.inmemory;

import com.enfos.reporting.domain.model.ColumnDefinition;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.query.FilterCriterion;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Compiles free-text search and column filters into row predicates for the in-memory
 * query engine.
 */
final class RowPredicateFactory {

    private RowPredicateFactory() {
    }

    static Predicate<ReportRow> searchPredicate(ReportDefinition definition, String term) {
        if (term == null || term.isBlank()) {
            return row -> true;
        }
        String needle = term.toLowerCase(Locale.ROOT);
        List<String> searchableKeys = definition.columns().stream()
                .filter(ColumnDefinition::searchable)
                .map(ColumnDefinition::key)
                .toList();

        return row -> searchableKeys.stream().anyMatch(key -> {
            Object value = row.value(key);
            return value != null && String.valueOf(value).toLowerCase(Locale.ROOT).contains(needle);
        });
    }

    static Predicate<ReportRow> filterPredicate(FilterCriterion criterion) {
        List<String> values = criterion.values();
        if (values.isEmpty()) {
            return row -> true;
        }
        // Exact match, not "contains" — a filter narrows to declared values (enum options
        // or an allowlisted text value), unlike search which looks for a substring.
        return row -> {
            Object value = row.value(criterion.columnKey());
            String stringValue = value == null ? null : String.valueOf(value);
            return values.stream().anyMatch(v -> v.equals(stringValue));
        };
    }

    static Predicate<ReportRow> filterPredicate(List<FilterCriterion> criteria) {
        Predicate<ReportRow> combined = row -> true;
        for (FilterCriterion criterion : criteria) {
            combined = combined.and(filterPredicate(criterion));
        }
        return combined;
    }
}
