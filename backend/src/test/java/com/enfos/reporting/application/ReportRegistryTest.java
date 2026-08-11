package com.enfos.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.enfos.reporting.domain.model.ColumnDefinition;
import com.enfos.reporting.domain.model.ColumnType;
import com.enfos.reporting.domain.model.FilterType;
import com.enfos.reporting.domain.model.Page;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.port.ReportDataSource;
import com.enfos.reporting.domain.port.ReportModule;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportRegistryTest {

    @Test
    void duplicateReportIdsFailConstructionNamingTheOffendingId() {
        ReportModule first = moduleWithId("users");
        ReportModule second = moduleWithId("users");

        assertThatIllegalStateException()
                .isThrownBy(() -> new ReportRegistry(List.of(first, second)))
                .withMessageContaining("users");
    }

    @Test
    void definitionsPreserveInjectionOrder() {
        ReportModule users = moduleWithId("users");
        ReportModule departments = moduleWithId("departments");

        ReportRegistry registry = new ReportRegistry(List.of(users, departments));

        assertThat(registry.definitions())
                .extracting(ReportDefinition::id)
                .containsExactly("users", "departments");
    }

    @Test
    void findReturnsEmptyForUnknownId() {
        ReportRegistry registry = new ReportRegistry(List.of(moduleWithId("users")));

        assertThat(registry.find("nope")).isEmpty();
        assertThat(registry.find("users")).isPresent();
    }

    private static ReportModule moduleWithId(String id) {
        ColumnDefinition idColumn =
                new ColumnDefinition("id", "ID", ColumnType.ID, true, false, FilterType.NONE, List.of());
        ReportDefinition definition = new ReportDefinition(id, id, "description", "category", List.of(idColumn));
        ReportDataSource dataSource = (def, query) -> new Page<ReportRow>(List.of(), 0, 25, 0);

        return new ReportModule() {
            @Override
            public ReportDefinition definition() {
                return definition;
            }

            @Override
            public ReportDataSource dataSource() {
                return dataSource;
            }
        };
    }
}
