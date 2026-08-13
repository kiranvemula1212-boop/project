package com.enfos.reporting.infrastructure.inmemory;

import com.enfos.reporting.domain.model.ColumnDefinition;
import com.enfos.reporting.domain.model.FilterType;
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

    static Predicate<ReportRow> filterPredicate(ReportDefinition definition, FilterCriterion criterion) {
        List<String> values = criterion.values();
        if (values.isEmpty()) {
            return row -> true;
        }

        // ENUM filters select from a fixed, declared set of values — exact match is
        // correct there. TEXT filters are free-typed by the user, so exact match is a
        // trap: typing a partial or differently-cased value would silently match
        // nothing, with no way for the user to tell why. Match ENUM and TEXT filters
        // the way each control actually invites the user to type into it.
        ColumnDefinition column = definition.column(criterion.columnKey())
                .orElseThrow(() -> new IllegalStateException(
                        "Unknown column '" + criterion.columnKey() + "' reached the data source unvalidated."));

        if (column.filterType() == FilterType.TEXT) {
            List<String> needles = values.stream().map(v -> v.toLowerCase(Locale.ROOT)).toList();
            return row -> {
                Object value = row.value(criterion.columnKey());
                if (value == null) {
                    return false;
                }
                String haystack = String.valueOf(value).toLowerCase(Locale.ROOT);
                return needles.stream().anyMatch(haystack::contains);
            };
        }

        return row -> {
            Object value = row.value(criterion.columnKey());
            String stringValue = value == null ? null : String.valueOf(value);
            return values.stream().anyMatch(v -> v.equals(stringValue));
        };
    }

    static Predicate<ReportRow> filterPredicate(ReportDefinition definition, List<FilterCriterion> criteria) {
        Predicate<ReportRow> combined = row -> true;
        for (FilterCriterion criterion : criteria) {
            combined = combined.and(filterPredicate(definition, criterion));
        }
        return combined;
    }
}
