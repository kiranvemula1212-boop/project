package com.enfos.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enfos.reporting.domain.model.ColumnDefinition;
import com.enfos.reporting.domain.model.ColumnType;
import com.enfos.reporting.domain.model.EnumOption;
import com.enfos.reporting.domain.model.FilterType;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.SortDirection;
import com.enfos.reporting.domain.query.FilterCriterion;
import com.enfos.reporting.domain.query.ReportQuery;
import com.enfos.reporting.domain.query.SortSpec;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryValidatorTest {

    private final QueryValidator validator = new QueryValidator(new ReportingProperties(25, 200));
    private final ReportDefinition definition = fixtureDefinition();

    @Test
    void unknownSortColumnIsRejected() {
        ReportQuery query = new ReportQuery(null, List.of(), List.of(new SortSpec("nope", SortDirection.ASC)), 0, 25);

        assertThatThrownBy(() -> validator.validate(definition, query))
                .isInstanceOf(InvalidQueryException.class)
                .satisfies(e -> assertThat(((InvalidQueryException) e).violations())
                        .anyMatch(v -> v.contains("Unknown column 'nope'") && v.contains("'fixtures'")));
    }

    @Test
    void nonSortableColumnIsRejected() {
        ReportQuery query = new ReportQuery(null, List.of(), List.of(new SortSpec("note", SortDirection.ASC)), 0, 25);

        assertThatThrownBy(() -> validator.validate(definition, query))
                .isInstanceOf(InvalidQueryException.class)
                .satisfies(e -> assertThat(((InvalidQueryException) e).violations())
                        .contains("Column 'note' is not sortable."));
    }

    @Test
    void nonFilterableColumnIsRejected() {
        ReportQuery query = new ReportQuery(
                null, List.of(new FilterCriterion("note", List.of("x"))), List.of(), 0, 25);

        assertThatThrownBy(() -> validator.validate(definition, query))
                .isInstanceOf(InvalidQueryException.class)
                .satisfies(e -> assertThat(((InvalidQueryException) e).violations())
                        .contains("Column 'note' is not filterable."));
    }

    @Test
    void enumFilterValueOutsideDeclaredOptionsIsRejectedNamingAllowedValues() {
        ReportQuery query = new ReportQuery(
                null, List.of(new FilterCriterion("department", List.of("HR"))), List.of(), 0, 25);

        assertThatThrownBy(() -> validator.validate(definition, query))
                .isInstanceOf(InvalidQueryException.class)
                .satisfies(e -> assertThat(((InvalidQueryException) e).violations())
                        .anyMatch(v -> v.contains("'HR'") && v.contains("ENG") && v.contains("SALES")));
    }

    @Test
    void oversizedPageIsRejected() {
        ReportQuery query = new ReportQuery(null, List.of(), List.of(), 0, 500);

        assertThatThrownBy(() -> validator.validate(definition, query))
                .isInstanceOf(InvalidQueryException.class)
                .satisfies(e -> assertThat(((InvalidQueryException) e).violations())
                        .contains("Page size must be between 1 and 200."));
    }

    @Test
    void negativePageIsRejected() {
        ReportQuery query = new ReportQuery(null, List.of(), List.of(), -1, 25);

        assertThatThrownBy(() -> validator.validate(definition, query))
                .isInstanceOf(InvalidQueryException.class)
                .satisfies(e -> assertThat(((InvalidQueryException) e).violations())
                        .contains("Page must be zero or greater."));
    }

    @Test
    void multipleViolationsAreReportedTogetherRatherThanFailingOnTheFirst() {
        ReportQuery query = new ReportQuery(
                null, List.of(), List.of(new SortSpec("nope", SortDirection.ASC)), -1, 500);

        assertThatThrownBy(() -> validator.validate(definition, query))
                .isInstanceOf(InvalidQueryException.class)
                .satisfies(e -> assertThat(((InvalidQueryException) e).violations()).hasSize(3));
    }

    @Test
    void validQueryPassesThroughUntouched() {
        ReportQuery query = new ReportQuery(
                "term",
                List.of(new FilterCriterion("department", List.of("ENG"))),
                List.of(new SortSpec("name", SortDirection.ASC)),
                0,
                25);

        assertThatCode(() -> validator.validate(definition, query)).doesNotThrowAnyException();
    }

    private static ReportDefinition fixtureDefinition() {
        List<ColumnDefinition> columns = List.of(
                new ColumnDefinition("id", "ID", ColumnType.ID, true, false, FilterType.NONE, List.of()),
                new ColumnDefinition("name", "Name", ColumnType.TEXT, true, true, FilterType.TEXT, List.of()),
                new ColumnDefinition("note", "Note", ColumnType.TEXT, false, false, FilterType.NONE, List.of()),
                new ColumnDefinition("department", "Department", ColumnType.TEXT, true, false, FilterType.ENUM,
                        List.of(new EnumOption("ENG", "Engineering"), new EnumOption("SALES", "Sales")))
        );
        return new ReportDefinition(
                "fixtures", "Fixtures", "Test fixture report", "test", LocalDate.of(2026, 1, 1), columns);
    }
}
