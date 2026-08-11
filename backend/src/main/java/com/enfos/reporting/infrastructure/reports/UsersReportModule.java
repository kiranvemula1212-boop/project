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
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 120 rows — enough for multi-page pagination and meaningful sorting, unlike the smaller
 * departments report.
 */
@Component
class UsersReportModule implements ReportModule {

    private final ReportDefinition definition;
    private final ReportDataSource dataSource;

    UsersReportModule(JsonSeedLoader seedLoader) {
        JsonSeedLoader.SeedData seed = seedLoader.load("data/users.json");
        this.definition = buildDefinition(seed.lastUpdated());
        this.dataSource = new InMemoryReportDataSource(seed.rows());
    }

    @Override
    public ReportDefinition definition() {
        return definition;
    }

    @Override
    public ReportDataSource dataSource() {
        return dataSource;
    }

    private static ReportDefinition buildDefinition(LocalDate lastUpdated) {
        List<ColumnDefinition> columns = List.of(
                new ColumnDefinition("id", "ID", ColumnType.ID, true, false, FilterType.NONE, List.of()),
                new ColumnDefinition("name", "Name", ColumnType.TEXT, true, true, FilterType.TEXT, List.of()),
                new ColumnDefinition("email", "Email", ColumnType.EMAIL, true, true, FilterType.NONE, List.of()),
                new ColumnDefinition("role", "Role", ColumnType.ENUM, true, false, FilterType.ENUM, List.of(
                        new EnumOption("ADMIN", "Admin"),
                        new EnumOption("MANAGER", "Manager"),
                        new EnumOption("ANALYST", "Analyst"),
                        new EnumOption("VIEWER", "Viewer"))),
                new ColumnDefinition(
                        "department", "Department", ColumnType.TEXT, true, true, FilterType.ENUM,
                        DepartmentOptions.ALL),
                new ColumnDefinition("status", "Status", ColumnType.ENUM, true, false, FilterType.ENUM, List.of(
                        new EnumOption("ACTIVE", "Active"),
                        new EnumOption("INACTIVE", "Inactive"),
                        new EnumOption("PENDING", "Pending"))),
                new ColumnDefinition(
                        "createdDate", "Created", ColumnType.DATE, true, false, FilterType.NONE, List.of())
        );
        return new ReportDefinition(
                "users", "Users", "All user accounts across the organization.", "People", lastUpdated, columns);
    }
}
