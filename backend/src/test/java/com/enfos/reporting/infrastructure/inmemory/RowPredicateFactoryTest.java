package com.enfos.reporting.infrastructure.inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import com.enfos.reporting.domain.model.ReportDefinition;
import com.enfos.reporting.domain.model.ReportRow;
import com.enfos.reporting.domain.query.FilterCriterion;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

class RowPredicateFactoryTest {

    private final ReportDefinition definition = TestReports.definition();

    @Test
    void searchMatchesSearchableColumnsCaseInsensitively() {
        ReportRow row = TestReports.row("1", "Alice Smith", "", "ENG", 10);

        assertThat(RowPredicateFactory.searchPredicate(definition, "alice").test(row)).isTrue();
        assertThat(RowPredicateFactory.searchPredicate(definition, "SMITH").test(row)).isTrue();
        assertThat(RowPredicateFactory.searchPredicate(definition, "bob").test(row)).isFalse();
    }

    @Test
    void searchIgnoresNonSearchableColumns() {
        // "note" is declared non-searchable; a term that only lives there must not match,
        // even though the term is literally present in the row's data.
        ReportRow row = TestReports.row("1", "Bob Jones", "secret-term", "ENG", 20);

        assertThat(RowPredicateFactory.searchPredicate(definition, "secret-term").test(row)).isFalse();
    }

    @Test
    void blankOrNullSearchTermMatchesEverything() {
        ReportRow row = TestReports.row("1", "Anyone", "", "ENG", 1);

        assertThat(RowPredicateFactory.searchPredicate(definition, "  ").test(row)).isTrue();
        assertThat(RowPredicateFactory.searchPredicate(definition, null).test(row)).isTrue();
    }

    @Test
    void multiValueFilterBehavesAsOrWithinAColumn() {
        // "department" is FilterType.ENUM in the fixture — exact match against declared values.
        Predicate<ReportRow> predicate = RowPredicateFactory.filterPredicate(
                definition, new FilterCriterion("department", List.of("ENG", "SALES")));

        assertThat(predicate.test(TestReports.row("1", "A", "", "ENG", 1))).isTrue();
        assertThat(predicate.test(TestReports.row("2", "B", "", "SALES", 1))).isTrue();
        assertThat(predicate.test(TestReports.row("3", "C", "", "HR", 1))).isFalse();
    }

    @Test
    void enumFilterIsExactAndCaseSensitiveNotContains() {
        // A partial or wrongly-cased value must NOT match — the ENUM control only ever
        // sends one of its declared option values, never free-typed text.
        Predicate<ReportRow> predicate =
                RowPredicateFactory.filterPredicate(definition, new FilterCriterion("department", List.of("eng")));

        assertThat(predicate.test(TestReports.row("1", "A", "", "ENG", 1))).isFalse();
    }

    @Test
    void textFilterMatchesCaseInsensitivePartialValues() {
        // "name" is FilterType.TEXT — the user free-types into this box, so it must behave
        // like search (contains, case-insensitive), not like an exact-match enum lookup.
        // This is the bug: typing "ali" into the Name filter used to match nothing at all.
        Predicate<ReportRow> predicate =
                RowPredicateFactory.filterPredicate(definition, new FilterCriterion("name", List.of("ali")));

        assertThat(predicate.test(TestReports.row("1", "Alice Smith", "", "ENG", 1))).isTrue();
        assertThat(predicate.test(TestReports.row("2", "ALICE JONES", "", "ENG", 1))).isTrue();
        assertThat(predicate.test(TestReports.row("3", "Bob Jones", "", "ENG", 1))).isFalse();
    }

    @Test
    void textFilterWithNoMatchingSubstringExcludesTheRow() {
        Predicate<ReportRow> predicate =
                RowPredicateFactory.filterPredicate(definition, new FilterCriterion("name", List.of("zzz")));

        assertThat(predicate.test(TestReports.row("1", "Alice Smith", "", "ENG", 1))).isFalse();
    }

    @Test
    void twoFiltersOnDifferentColumnsBehaveAsAnd() {
        List<FilterCriterion> criteria = List.of(
                new FilterCriterion("department", List.of("ENG")),
                new FilterCriterion("name", List.of("Alice"))
        );
        Predicate<ReportRow> predicate = RowPredicateFactory.filterPredicate(definition, criteria);

        assertThat(predicate.test(TestReports.row("1", "Alice", "", "ENG", 1))).isTrue();
        assertThat(predicate.test(TestReports.row("2", "Alice", "", "SALES", 1))).isFalse();
        assertThat(predicate.test(TestReports.row("3", "Bob", "", "ENG", 1))).isFalse();
    }
}
