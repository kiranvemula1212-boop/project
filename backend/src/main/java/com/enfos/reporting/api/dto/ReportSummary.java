package com.enfos.reporting.api.dto;

import com.enfos.reporting.domain.model.ReportDefinition;
import java.time.LocalDate;

public record ReportSummary(
        String id, String name, String description, String category, LocalDate lastUpdated, int columnCount) {

    public static ReportSummary from(ReportDefinition definition) {
        return new ReportSummary(
                definition.id(),
                definition.name(),
                definition.description(),
                definition.category(),
                definition.lastUpdated(),
                definition.columns().size());
    }
}
