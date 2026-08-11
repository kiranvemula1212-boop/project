package com.enfos.reporting.infrastructure.inmemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.enfos.reporting.domain.model.ReportRow;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonSeedLoaderTest {

    private final JsonSeedLoader loader = new JsonSeedLoader();

    @Test
    void loadsRowsFromClasspathJson() {
        List<ReportRow> rows = loader.load("data/seed-fixture.json");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).value("id")).isEqualTo("1");
        assertThat(rows.get(0).value("date")).isEqualTo("2024-01-01");
    }

    @Test
    void numbersDeserializeAsIntegerOrBigDecimalNeverDouble() {
        List<ReportRow> rows = loader.load("data/seed-fixture.json");

        assertThat(rows.get(0).value("count")).isInstanceOf(Integer.class);
        assertThat(rows.get(0).value("amount")).isInstanceOf(BigDecimal.class);
    }

    @Test
    void missingFileThrowsNamingTheFile() {
        assertThatIllegalStateException()
                .isThrownBy(() -> loader.load("data/does-not-exist.json"))
                .withMessageContaining("does-not-exist.json");
    }

    @Test
    void malformedFileThrowsNamingTheFile() {
        assertThatIllegalStateException()
                .isThrownBy(() -> loader.load("data/malformed-fixture.json"))
                .withMessageContaining("malformed-fixture.json");
    }
}
