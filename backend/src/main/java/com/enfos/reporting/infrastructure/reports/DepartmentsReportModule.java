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
 * 12 rows — deliberately fewer than one page, proving the pagination control degrades
 * gracefully instead of rendering an awkward "Page 1 of 1" with dead buttons.
 */
@Component
class DepartmentsReportModule implements ReportModule {

    private final ReportDefinition definition = buildDefinition();
    private final ReportDataSource dataSource;

    DepartmentsReportModule(JsonSeedLoader seedLoader) {
        this.dataSource = new InMemoryReportDataSource(seedLoader.load("data/departments.json"));
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
                new ColumnDefinition("manager", "Manager", ColumnType.TEXT, true, true, FilterType.NONE, List.of()),
                new ColumnDefinition(
                        "employeeCount", "Employees", ColumnType.NUMBER, true, false, FilterType.NONE, List.of()),
                new ColumnDefinition("location", "Location", ColumnType.ENUM, true, false, FilterType.ENUM, List.of(
                        new EnumOption("Austin, TX", "Austin, TX"),
                        new EnumOption("Denver, CO", "Denver, CO"),
                        new EnumOption("Seattle, WA", "Seattle, WA"),
                        new EnumOption("Chicago, IL", "Chicago, IL"),
                        new EnumOption("Atlanta, GA", "Atlanta, GA"),
                        new EnumOption("Boston, MA", "Boston, MA"))),
                new ColumnDefinition(
                        "annualBudget", "Annual Budget", ColumnType.CURRENCY, true, false, FilterType.NONE,
                        List.of())
        );
        return new ReportDefinition(
                "departments", "Departments", "Organizational departments and their budgets.", "People", columns);
    }
}
