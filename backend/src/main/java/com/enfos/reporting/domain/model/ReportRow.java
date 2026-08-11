package com.enfos.reporting.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single row of report data, deliberately typed as {@code Map<String, Object>} rather
 * than a typed entity. The table renders any report, so rows must be schema-agnostic at
 * the transport layer; type safety is recovered by the {@link ColumnDefinition} contract,
 * which is validated at startup. A generic {@code ReportRow<T>} over typed entities was
 * rejected — it would force the controller, service, and serializer to know every report
 * type, which defeats the point of the metadata-driven design.
 */
public record ReportRow(Map<String, Object> values) {

    public ReportRow {
        // LinkedHashMap + unmodifiableMap, not Map.copyOf: column order must survive
        // (it drives JSON field order), and Map.copyOf does not guarantee iteration order.
        values = values == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public static ReportRow of(Map<String, Object> values) {
        return new ReportRow(values);
    }

    public Object value(String columnKey) {
        return values.get(columnKey);
    }
}
