package com.enfos.reporting.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportDefinitionTest {

    private static final ColumnDefinition ID_COLUMN =
            new ColumnDefinition("id", "ID", ColumnType.ID, true, false, FilterType.NONE, List.of());
    private static final LocalDate LAST_UPDATED = LocalDate.of(2026, 1, 1);

    @Test
    void idMustMatchUrlSafeSlugPattern() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ReportDefinition(
                "Users", "Users", "desc", "cat", LAST_UPDATED, List.of(ID_COLUMN)));
    }

    @Test
    void mustDeclareLastUpdated() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new ReportDefinition("users", "Users", "desc", "cat", null, List.of(ID_COLUMN)));
    }

    @Test
    void mustDeclareAtLeastOneColumn() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new ReportDefinition("users", "Users", "desc", "cat", LAST_UPDATED, List.of()));
    }

    @Test
    void columnKeysMustBeUnique() {
        ColumnDefinition duplicate =
                new ColumnDefinition("id", "ID 2", ColumnType.TEXT, false, false, FilterType.NONE, List.of());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ReportDefinition(
                        "users", "Users", "desc", "cat", LAST_UPDATED, List.of(ID_COLUMN, duplicate)))
                .withMessageContaining("id");
    }

    @Test
    void columnLooksUpByKey() {
        ReportDefinition definition =
                new ReportDefinition("users", "Users", "desc", "cat", LAST_UPDATED, List.of(ID_COLUMN));

        assertThat(definition.column("id")).isPresent();
        assertThat(definition.column("missing")).isEmpty();
    }
}
