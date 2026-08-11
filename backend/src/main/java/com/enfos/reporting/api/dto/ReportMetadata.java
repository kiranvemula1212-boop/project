package com.enfos.reporting.api.dto;

import com.enfos.reporting.domain.model.ColumnDefinition;
import com.enfos.reporting.domain.model.ReportDefinition;
import java.util.List;

public record ReportMetadata(String id, String name, String description, String category, List<ColumnDefinition> columns) {

    public static ReportMetadata from(ReportDefinition definition) {
        return new ReportMetadata(
                definition.id(),
                definition.name(),
                definition.description(),
                definition.category(),
                definition.columns());
    }
}
