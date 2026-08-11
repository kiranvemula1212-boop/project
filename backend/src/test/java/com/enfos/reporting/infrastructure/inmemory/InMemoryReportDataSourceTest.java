package com.enfos.reporting.infrastructure.inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import com.enfos.reporting.domain.model.Page;
import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.model.SortDirection;
import com.enfos.reporting.domain.query.FilterCriterion;
import com.enfos.reporting.domain.query.ReportQuery;
import com.enfos.reporting.domain.query.SortSpec;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryReportDataSourceTest {

    private final ReportDefinition definition = TestReports.definition();

    @Test
    void twoRowsWithEqualSortKeysKeepStableRelativeOrderAcrossPageBoundaries() {
        // Five rows tied on "score" — a fixture built specifically so the tie spans a
        // page edge (page size 2). Without the identity-column tiebreaker, ties can
        // reorder between requests and a row gets skipped or duplicated across pages.
        List<ReportRow> rows = List.of(
                TestReports.row("1", "A", "", "ENG", 5),
                TestReports.row("2", "B", "", "ENG", 5),
                TestReports.row("3", "C", "", "ENG", 5),
                TestReports.row("4", "D", "", "ENG", 5),
                TestReports.row("5", "E", "", "ENG", 5)
        );
        InMemoryReportDataSource dataSource = new InMemoryReportDataSource(rows);
        List<SortSpec> sorts = List.of(new SortSpec("score", SortDirection.ASC));

        Page<ReportRow> page0 = dataSource.fetch(definition, new ReportQuery(null, List.of(), sorts, 0, 2));
        Page<ReportRow> page1 = dataSource.fetch(definition, new ReportQuery(null, List.of(), sorts, 1, 2));
        Page<ReportRow> page2 = dataSource.fetch(definition, new ReportQuery(null, List.of(), sorts, 2, 2));

        assertThat(page0.content()).extracting(r -> r.value("id")).containsExactly("1", "2");
        assertThat(page1.content()).extracting(r -> r.value("id")).containsExactly("3", "4");
        assertThat(page2.content()).extracting(r -> r.value("id")).containsExactly("5");
    }

    @Test
    void pagePastTheEndReturnsEmptyContentWithCorrectTotalElements() {
        List<ReportRow> rows = List.of(
                TestReports.row("1", "A", "", "ENG", 1),
                TestReports.row("2", "B", "", "ENG", 2)
        );
        InMemoryReportDataSource dataSource = new InMemoryReportDataSource(rows);

        Page<ReportRow> page = dataSource.fetch(definition, new ReportQuery(null, List.of(), List.of(), 5, 10));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void totalElementsReflectsFilteredCountNotRawCount() {
        List<ReportRow> rows = List.of(
                TestReports.row("1", "A", "", "ENG", 1),
                TestReports.row("2", "B", "", "SALES", 2),
                TestReports.row("3", "C", "", "ENG", 3)
        );
        InMemoryReportDataSource dataSource = new InMemoryReportDataSource(rows);
        ReportQuery query = new ReportQuery(
                null, List.of(new FilterCriterion("department", List.of("ENG"))), List.of(), 0, 10);

        Page<ReportRow> page = dataSource.fetch(definition, query);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).hasSize(2);
    }
}
