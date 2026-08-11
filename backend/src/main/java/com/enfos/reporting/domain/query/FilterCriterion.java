package com.enfos.reporting.domain.query;

import java.util.List;

/**
 * A single column's active filter values. Multiple values mean OR within the column;
 * combining multiple criteria (see {@link ReportQuery}) means AND across columns.
 */
public record FilterCriterion(String columnKey, List<String> values) {

    public FilterCriterion {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
