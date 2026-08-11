package com.enfos.reporting.api.dto;

import com.enfos.reporting.domain.model.ReportDefinition;

public record ReportSummary(String id, String name, String description, String category, int columnCount) {

    public static ReportSummary from(ReportDefinition definition) {
        return new ReportSummary(
                definition.id(),
                definition.name(),
                definition.description(),
                definition.category(),
                definition.columns().size());
    }
}
