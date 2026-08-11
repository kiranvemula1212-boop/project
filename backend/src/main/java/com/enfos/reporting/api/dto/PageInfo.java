package com.enfos.reporting.api.dto;

import com.enfos.reporting.domain.model.Page;

public record PageInfo(int number, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {

    public static PageInfo from(Page<?> page) {
        return new PageInfo(
                page.number(), page.size(), page.totalElements(), page.totalPages(), page.hasNext(), page.hasPrevious());
    }
}
