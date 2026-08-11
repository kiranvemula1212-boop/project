package com.enfos.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enfos.reporting.domain.model.ColumnDefinition;
import com.enfos.reporting.domain.model.ColumnType;
import com.enfos.reporting.domain.model.FilterType;
import com.enfos.reporting.domain.model.Page;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.model.SortDirection;
import com.enfos.reporting.domain.port.ReportDataSource;
import com.enfos.reporting.domain.port.ReportModule;
import com.enfos.reporting.domain.query.ReportQuery;
import com.enfos.reporting.domain.query.SortSpec;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ReportServiceTest {

    private final ReportingProperties properties = new ReportingProperties(25, 200);

    @Test
    void getDefinitionThrowsReportNotFoundExceptionNamingTheId() {
        ReportService service = new ReportService(new ReportRegistry(List.of()), new QueryValidator(properties));

        assertThatThrownBy(() -> service.getDefinition("nope"))
                .isInstanceOf(ReportNotFoundException.class)
                .satisfies(e -> assertThat(((ReportNotFoundException) e).reportId()).isEqualTo("nope"));
    }

    @Test
    void getDataThrowsInvalidQueryExceptionWithoutEverCallingTheDataSource() {
        AtomicReference<ReportQuery> received = new AtomicReference<>();
        ReportModule module = fixtureModule(received);
        ReportService service = new ReportService(
                new ReportRegistry(List.of(module)), new QueryValidator(properties));

        ReportQuery invalidQuery = new ReportQuery(
                null, List.of(), List.of(new SortSpec("nope", SortDirection.ASC)), 0, 25);

        assertThatThrownBy(() -> service.getData("fixtures", invalidQuery))
                .isInstanceOf(InvalidQueryException.class);
        assertThat(received.get()).isNull();
    }

    @Test
    void getDataDelegatesTheValidatedQueryToTheModulesDataSource() {
        AtomicReference<ReportQuery> received = new AtomicReference<>();
        ReportModule module = fixtureModule(received);
        ReportService service = new ReportService(
                new ReportRegistry(List.of(module)), new QueryValidator(properties));

        ReportQuery validQuery = new ReportQuery(null, List.of(), List.of(), 0, 25);
        Page<ReportRow> result = service.getData("fixtures", validQuery);

        assertThat(received.get()).isEqualTo(validQuery);
        assertThat(result.content()).isEmpty();
    }

    private static ReportModule fixtureModule(AtomicReference<ReportQuery> receivedQuery) {
        ColumnDefinition idColumn =
                new ColumnDefinition("id", "ID", ColumnType.ID, true, false, FilterType.NONE, List.of());
        ReportDefinition definition =
                new ReportDefinition("fixtures", "Fixtures", "description", "category", List.of(idColumn));
        ReportDataSource dataSource = (def, query) -> {
            receivedQuery.set(query);
            return new Page<>(List.of(), query.page(), query.size(), 0);
        };

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
