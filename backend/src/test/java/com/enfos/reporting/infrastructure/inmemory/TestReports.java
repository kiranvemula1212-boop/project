package com.enfos.reporting.infrastructure.inmemory;

import com.enfos.reporting.domain.model.ColumnDefinition;
import com.enfos.reporting.domain.model.ColumnType;
import com.enfos.reporting.domain.model.EnumOption;
import com.enfos.reporting.domain.model.FilterType;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared fixture report used across the in-memory query engine tests. */
final class TestReports {

    private TestReports() {
    }

    static ReportDefinition definition() {
        List<ColumnDefinition> columns = List.of(
                new ColumnDefinition("id", "ID", ColumnType.ID, true, false, FilterType.NONE, List.of()),
                new ColumnDefinition("name", "Name", ColumnType.TEXT, true, true, FilterType.TEXT, List.of()),
                new ColumnDefinition("note", "Note", ColumnType.TEXT, false, false, FilterType.NONE, List.of()),
                new ColumnDefinition("department", "Department", ColumnType.TEXT, true, false, FilterType.ENUM,
                        List.of(new EnumOption("ENG", "Engineering"), new EnumOption("SALES", "Sales"))),
                new ColumnDefinition("score", "Score", ColumnType.NUMBER, true, false, FilterType.NONE, List.of())
        );
        return new ReportDefinition(
                "fixtures", "Fixtures", "Test fixture report", "test", LocalDate.of(2026, 1, 1), columns);
    }

    static ReportRow row(String id, String name, String note, String department, Integer score) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("name", name);
        values.put("note", note);
        values.put("department", department);
        values.put("score", score);
        return ReportRow.of(values);
    }
}
