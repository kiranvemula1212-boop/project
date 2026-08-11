package com.enfos.reporting.infrastructure.reports;

import com.enfos.reporting.domain.model.ColumnDefinition;
import com.enfos.reporting.domain.model.ColumnType;
import com.enfos.reporting.domain.model.EnumOption;
import com.enfos.reporting.domain.model.FilterType;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.port.ReportDataSource;
import com.enfos.reporting.domain.port.ReportModule;
import com.enfos.reporting.infrastructure.inmemory.InMemoryReportDataSource;
import com.enfos.reporting.infrastructure.inmemory.JsonSeedLoader;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 60 rows with a nullable {@code endDate} and five status values — the richest report for
 * exercising filtering and the "no end date yet" null-handling in the query engine.
 */
@Component
class ProjectsReportModule implements ReportModule {

    private final ReportDefinition definition = buildDefinition();
    private final ReportDataSource dataSource;

    ProjectsReportModule(JsonSeedLoader seedLoader) {
        this.dataSource = new InMemoryReportDataSource(seedLoader.load("data/projects.json"));
    }

    @Override
    public ReportDefinition definition() {
        return definition;
    }

    @Override
    public ReportDataSource dataSource() {
        return dataSource;
    }

    private static ReportDefinition buildDefinition() {
        List<ColumnDefinition> columns = List.of(
                new ColumnDefinition("id", "ID", ColumnType.ID, true, false, FilterType.NONE, List.of()),
                new ColumnDefinition("name", "Name", ColumnType.TEXT, true, true, FilterType.NONE, List.of()),
                new ColumnDefinition(
                        "department", "Department", ColumnType.TEXT, true, false, FilterType.ENUM,
                        DepartmentOptions.ALL),
                new ColumnDefinition("owner", "Owner", ColumnType.TEXT, true, true, FilterType.NONE, List.of()),
                new ColumnDefinition("status", "Status", ColumnType.ENUM, true, false, FilterType.ENUM, List.of(
                        new EnumOption("PLANNING", "Planning"),
                        new EnumOption("ACTIVE", "Active"),
                        new EnumOption("ON_HOLD", "On Hold"),
                        new EnumOption("COMPLETED", "Completed"),
                        new EnumOption("CANCELLED", "Cancelled"))),
                new ColumnDefinition("startDate", "Start Date", ColumnType.DATE, true, false, FilterType.NONE,
                        List.of()),
                new ColumnDefinition("endDate", "End Date", ColumnType.DATE, true, false, FilterType.NONE, List.of())
        );
        return new ReportDefinition(
                "projects", "Projects", "Active and historical projects across departments.", "Work", columns);
    }
}
