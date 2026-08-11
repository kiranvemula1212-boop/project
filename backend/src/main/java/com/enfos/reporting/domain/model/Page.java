package com.enfos.reporting.domain.model;

import java.util.List;

public record Page<T>(List<T> content, int number, int size, long totalElements) {

    public Page {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public int totalPages() {
        // A report with zero rows is still "Page 1 of 1" in the UI, not "Page 1 of 0" —
        // there is no such thing as a page count of zero for a successful, empty answer.
        if (totalElements == 0) {
            return 1;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return number < totalPages() - 1;
    }

    public boolean hasPrevious() {
        return number > 0;
    }
}
