package com.enfos.reporting.application;

import com.enfos.reporting.domain.model.ColumnDefinition;
import com.enfos.reporting.domain.model.EnumOption;
import com.enfos.reporting.domain.model.FilterType;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.query.FilterCriterion;
import com.enfos.reporting.domain.query.ReportQuery;
import com.enfos.reporting.domain.query.SortSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The security boundary of the backend: filter keys, sort keys, and sort directions
 * arrive as user-controlled strings that would land in SQL identifier position in any
 * future JDBC adapter, a place bind parameters cannot protect. Validating here, against
 * exactly the report's declared columns, means every present and future
 * {@code ReportDataSource} adapter inherits the guarantee automatically — an adapter
 * author cannot forget to check what a request is asking to touch.
 */
@Component
class QueryValidator {

    private final ReportingProperties properties;

    QueryValidator(ReportingProperties properties) {
        this.properties = properties;
    }

    void validate(ReportDefinition definition, ReportQuery query) {
        List<String> violations = new ArrayList<>();

        if (query.page() < 0) {
            violations.add("Page must be zero or greater.");
        }
        if (query.size() < 1 || query.size() > properties.maxPageSize()) {
            violations.add("Page size must be between 1 and " + properties.maxPageSize() + ".");
        }

        for (SortSpec sort : query.sorts()) {
            validateSort(definition, sort, violations);
        }
        for (FilterCriterion filter : query.filters()) {
            validateFilter(definition, filter, violations);
        }

        // Page size is rejected, not clamped, when too large: silently returning fewer
        // rows than requested would make a client believe it has all the data.
        if (!violations.isEmpty()) {
            throw new InvalidQueryException(violations);
        }
    }

    private void validateSort(ReportDefinition definition, SortSpec sort, List<String> violations) {
        Optional<ColumnDefinition> column = definition.column(sort.columnKey());
        if (column.isEmpty()) {
            violations.add(unknownColumnMessage(definition, sort.columnKey()));
            return;
        }
        if (!column.get().sortable()) {
            violations.add("Column '" + sort.columnKey() + "' is not sortable.");
        }
    }

    private void validateFilter(ReportDefinition definition, FilterCriterion filter, List<String> violations) {
        Optional<ColumnDefinition> columnLookup = definition.column(filter.columnKey());
        if (columnLookup.isEmpty()) {
            violations.add(unknownColumnMessage(definition, filter.columnKey()));
            return;
        }

        ColumnDefinition column = columnLookup.get();
        if (column.filterType() == FilterType.NONE) {
            violations.add("Column '" + filter.columnKey() + "' is not filterable.");
            return;
        }

        if (column.filterType() == FilterType.ENUM) {
            List<String> allowedValues = column.options().stream().map(EnumOption::value).toList();
            for (String value : filter.values()) {
                if (!allowedValues.contains(value)) {
                    violations.add("Column '" + filter.columnKey() + "' does not allow value '" + value
                            + "'. Allowed values: " + String.join(", ", allowedValues) + ".");
                }
            }
        }
    }

    private static String unknownColumnMessage(ReportDefinition definition, String columnKey) {
        return "Unknown column '" + columnKey + "' on report '" + definition.id() + "'.";
    }
}
