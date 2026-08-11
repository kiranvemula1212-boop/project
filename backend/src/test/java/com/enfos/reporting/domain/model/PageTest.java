package com.enfos.reporting.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageTest {

    @Test
    void totalPagesIsOneWhenThereAreZeroElements() {
        Page<String> page = new Page<>(List.of(), 0, 25, 0);

        assertThat(page.totalPages()).isEqualTo(1);
        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void totalPagesRoundsUpPartialLastPage() {
        Page<String> page = new Page<>(List.of("a", "b"), 0, 25, 51);

        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void hasNextIsTrueWhenNotOnTheLastPage() {
        Page<String> page = new Page<>(List.of("a"), 0, 25, 51);

        assertThat(page.hasNext()).isTrue();
        assertThat(page.hasPrevious()).isFalse();
    }

    @Test
    void hasNextIsFalseOnTheLastPage() {
        Page<String> page = new Page<>(List.of("a"), 2, 25, 51);

        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    void contentDefaultsToEmptyWhenConstructedWithNull() {
        Page<String> page = new Page<>(null, 0, 25, 0);

        assertThat(page.content()).isEmpty();
    }
}
