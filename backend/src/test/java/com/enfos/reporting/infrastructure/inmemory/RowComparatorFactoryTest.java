package com.enfos.reporting.infrastructure.inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.model.SortDirection;
import com.enfos.reporting.domain.query.SortSpec;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class RowComparatorFactoryTest {

    private final ReportDefinition definition = TestReports.definition();

    @Test
    void sortsAscendingAndDescendingOnATextColumn() {
        List<ReportRow> rows = List.of(
                TestReports.row("1", "Charlie", "", "ENG", 1),
                TestReports.row("2", "Alice", "", "ENG", 1),
                TestReports.row("3", "Bob", "", "ENG", 1)
        );

        Comparator<ReportRow> ascending =
                RowComparatorFactory.comparatorFor(definition, List.of(new SortSpec("name", SortDirection.ASC)));
        assertThat(rows.stream().sorted(ascending).toList())
                .extracting(r -> r.value("name"))
                .containsExactly("Alice", "Bob", "Charlie");

        Comparator<ReportRow> descending =
                RowComparatorFactory.comparatorFor(definition, List.of(new SortSpec("name", SortDirection.DESC)));
        assertThat(rows.stream().sorted(descending).toList())
                .extracting(r -> r.value("name"))
                .containsExactly("Charlie", "Bob", "Alice");
    }

    @Test
    void nullsSortLastInBothDirections() {
        List<ReportRow> rows = List.of(
                TestReports.row("1", "A", "", "ENG", null),
                TestReports.row("2", "B", "", "ENG", 5),
                TestReports.row("3", "C", "", "ENG", 1)
        );

        Comparator<ReportRow> ascending =
                RowComparatorFactory.comparatorFor(definition, List.of(new SortSpec("score", SortDirection.ASC)));
        assertThat(rows.stream().sorted(ascending).toList())
                .extracting(r -> r.value("score"))
                .containsExactly(1, 5, null);

        Comparator<ReportRow> descending =
                RowComparatorFactory.comparatorFor(definition, List.of(new SortSpec("score", SortDirection.DESC)));
        assertThat(rows.stream().sorted(descending).toList())
                .extracting(r -> r.value("score"))
                .containsExactly(5, 1, null);
    }

    @Test
    void appendsIdentityColumnAsStableTiebreakerEvenWithNoSortsRequested() {
        List<ReportRow> rows = List.of(
                TestReports.row("3", "Same", "", "ENG", 1),
                TestReports.row("1", "Same", "", "ENG", 1),
                TestReports.row("2", "Same", "", "ENG", 1)
        );

        Comparator<ReportRow> comparator = RowComparatorFactory.comparatorFor(definition, List.of());

        assertThat(rows.stream().sorted(comparator).toList())
                .extracting(r -> r.value("id"))
                .containsExactly("1", "2", "3");
    }
}
