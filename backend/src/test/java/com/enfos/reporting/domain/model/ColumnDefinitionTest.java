package com.enfos.reporting.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

class ColumnDefinitionTest {

    @Test
    void enumFilterTypeWithoutOptionsFailsAtConstruction() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ColumnDefinition(
                        "status", "Status", ColumnType.ENUM, true, false, FilterType.ENUM, List.of()))
                .withMessageContaining("status");
    }

    @Test
    void nonEnumFilterTypeWithOptionsFailsAtConstruction() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ColumnDefinition(
                        "name", "Name", ColumnType.TEXT, true, true, FilterType.TEXT,
                        List.of(new EnumOption("x", "X"))))
                .withMessageContaining("name");
    }

    @Test
    void blankKeyFailsAtConstruction() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ColumnDefinition(
                        " ", "Name", ColumnType.TEXT, true, true, FilterType.NONE, List.of()));
    }

    @Test
    void validEnumColumnConstructsSuccessfully() {
        ColumnDefinition column = new ColumnDefinition(
                "status", "Status", ColumnType.ENUM, true, false, FilterType.ENUM,
                List.of(new EnumOption("ACTIVE", "Active")));

        assertThat(column.options()).hasSize(1);
    }
}
