package com.enfos.reporting.domain.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Describes how a report presents itself: its identity in the URL, its display metadata,
 * and its columns. Deliberately holds no data access logic — that lives behind
 * {@code ReportDataSource}, keeping "what a report looks like" separate from "where its
 * rows come from". {@code lastUpdated} describes the report's data, not this metadata
 * record itself — when the underlying dataset was last refreshed, not when a column was
 * relabeled.
 */
public record ReportDefinition(
        String id,
        String name,
        String description,
        String category,
        LocalDate lastUpdated,
        List<ColumnDefinition> columns
) {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-z][a-z0-9-]*$");

    public ReportDefinition {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(
                    "ReportDefinition id '" + id + "' must match ^[a-z][a-z0-9-]*$.");
        }
        if (lastUpdated == null) {
            throw new IllegalArgumentException("ReportDefinition '" + id + "' must declare lastUpdated.");
        }
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "ReportDefinition '" + id + "' must declare at least one column.");
        }
        columns = List.copyOf(columns);

        Set<String> seenKeys = new HashSet<>();
        for (ColumnDefinition column : columns) {
            if (!seenKeys.add(column.key())) {
                throw new IllegalArgumentException(
                        "ReportDefinition '" + id + "' has duplicate column key '" + column.key() + "'.");
            }
        }
    }

    public Optional<ColumnDefinition> column(String key) {
        return columns.stream().filter(c -> c.key().equals(key)).findFirst();
    }
}
