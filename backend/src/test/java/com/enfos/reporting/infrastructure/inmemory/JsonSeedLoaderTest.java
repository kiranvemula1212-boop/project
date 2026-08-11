package com.enfos.reporting.infrastructure.inmemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class JsonSeedLoaderTest {

    private final JsonSeedLoader loader = new JsonSeedLoader();

    @Test
    void loadsLastUpdatedAndRowsFromClasspathJson() {
        JsonSeedLoader.SeedData seed = loader.load("data/seed-fixture.json");

        assertThat(seed.lastUpdated()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(seed.rows()).hasSize(2);
        assertThat(seed.rows().get(0).value("id")).isEqualTo("1");
        assertThat(seed.rows().get(0).value("date")).isEqualTo("2024-01-01");
    }

    @Test
    void numbersDeserializeAsIntegerOrBigDecimalNeverDouble() {
        JsonSeedLoader.SeedData seed = loader.load("data/seed-fixture.json");

        assertThat(seed.rows().get(0).value("count")).isInstanceOf(Integer.class);
        assertThat(seed.rows().get(0).value("amount")).isInstanceOf(BigDecimal.class);
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

    @Test
    void fileMissingLastUpdatedOrRowsThrows() {
        assertThatIllegalStateException()
                .isThrownBy(() -> loader.load("data/incomplete-fixture.json"))
                .withMessageContaining("incomplete-fixture.json");
    }
}
