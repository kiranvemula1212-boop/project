package com.enfos.reporting.domain.model;

import java.util.List;

/**
 * Describes how a single column of a report is presented: its rendering type, whether it
 * can be sorted or searched, and what filter control (if any) it exposes.
 *
 * {@code type} drives rendering and {@code filterType} drives the filter control as two
 * separate fields on purpose — a DATE column renders as a formatted date but would filter
 * as a range, so collapsing the two into one enum would be an immediate design smell.
 */
public record ColumnDefinition(
        String key,
        String label,
        ColumnType type,
        boolean sortable,
        boolean searchable,
        FilterType filterType,
        List<EnumOption> options
) {

    public ColumnDefinition {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("ColumnDefinition key must not be blank.");
        }
        options = options == null ? List.of() : List.copyOf(options);

        // Fail fast at startup rather than at request time: an ENUM column with no
        // declared options, or a non-ENUM column with stray options, is a wiring bug in
        // a ReportModule, not a runtime condition a client can trigger.
        if (filterType == FilterType.ENUM && options.isEmpty()) {
            throw new IllegalArgumentException(
                    "Column '" + key + "' has filterType ENUM but declares no options.");
        }
        if (filterType != FilterType.ENUM && !options.isEmpty()) {
            throw new IllegalArgumentException(
                    "Column '" + key + "' declares options but filterType is not ENUM.");
        }
    }
}
